package com.example.mdv02batch.injector;

import java.time.Duration;
import java.util.List;

import javax.sql.DataSource;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.dto.ContractEntity;
import com.example.mdv02batch.injector.dto.CtrBlock;
import com.example.mdv02batch.injector.listener.CtrBlockSkipListener;
import com.example.mdv02batch.injector.processor.CtrBlockItemProcessor;
import com.example.mdv02batch.injector.reader.CtrBlockItemReader;
import com.example.mdv02batch.injector.reader.InjectorBusinessDataLineMapper;
import com.example.mdv02batch.injector.writer.CtrBlockLineAggregator;
import com.example.mdv02batch.injector.writer.CtrBlockToContractEntityConverter;
import com.example.mdv02batch.injector.writer.InjectorLineAggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JobInjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInjector.class);

    private final int chunkSize;
    private final String separator;
    private final String rootRecordType;
    private final int skipLimit;

    public JobInjector(
            @Value("${batch.injector.chunk-size:500}") int chunkSize,
            @Value("${batch.injector.separator:;}") String separator,
            @Value("${batch.injector.root-record-type:CTR}") String rootRecordType,
            @Value("${batch.injector.skip-limit:50000}") int skipLimit) {
        this.chunkSize = chunkSize;
        this.separator = separator;
        this.rootRecordType = rootRecordType;
        this.skipLimit = skipLimit;
    }

    @Bean
    public Job injectorJob(JobRepository jobRepository, Step injectorStep) {
        return new JobBuilder("injectorJob", jobRepository)
                .listener(jobExecutionListener())
                .start(injectorStep)
                .build();
    }

    /**
     * The item of the step is a CTR block, no longer a physical line: one chunk
     * therefore commits a whole number of contracts, never a truncated one.
     *
     * <p>Writer is a {@link CompositeItemWriter} that delegates to both a file
     * writer and a JDBC database writer.</p>
     *
     * <p>Fault tolerance is enabled so that any bad contract block (e.g. database
     * formatting error or exception during writing/processing) is skipped up to
     * {@code skipLimit} without stopping the batch process.</p>
     */
    @Bean
    public Step injectorStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             CtrBlockItemReader injectorBlockReader,
                             CtrBlockItemProcessor injectorProcessor,
                             CompositeItemWriter<CtrBlock> injectorCompositeWriter,
                             CtrBlockSkipListener skipListener) {
        return new StepBuilder("injectorStep", jobRepository)
                .<CtrBlock, CtrBlock>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(injectorBlockReader)
                .processor(injectorProcessor)
                .writer(injectorCompositeWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(skipLimit)
                .listener(skipListener)
                .listener(stepExecutionListener())
                .build();
    }

    /**
     * Line-level reader, kept unchanged: it still maps one physical line to one
     * {@link BusinessDataLine}. It is no longer wired directly into the step but
     * wrapped by {@link CtrBlockItemReader}.
     */
    @Bean
    @StepScope
    public FlatFileItemReader<BusinessDataLine> injectorLineReader(
            @Value("#{jobParameters['inputFile'] ?: '${batch.injector.input-file}'}") Resource inputFile) {
        LOGGER.info("Configuring injectorLineReader with inputFile={}", inputFile);
        return new FlatFileItemReaderBuilder<BusinessDataLine>()
                .name("injectorLineReader")
                .resource(inputFile)
                .encoding("UTF-8")
                .lineMapper(new InjectorBusinessDataLineMapper(separator))
                .build();
    }

    /** Groups the lines of the flat file into CTR blocks. */
    @Bean
    @StepScope
    public CtrBlockItemReader injectorBlockReader(
            FlatFileItemReader<BusinessDataLine> injectorLineReader) {
        LOGGER.info("Configuring injectorBlockReader with rootRecordType={}", rootRecordType);
        return new CtrBlockItemReader(injectorLineReader, rootRecordType);
    }

    /**
     * The aggregator is now block-scoped: {@link CtrBlockLineAggregator} renders
     * the whole block, delegating each line to {@link InjectorLineAggregator}.
     */
    @Bean
    @StepScope
    public FlatFileItemWriter<CtrBlock> injectorFileWriter(
            @Value("#{jobParameters['outputFile'] ?: '${batch.injector.output-file}'}") String outputFile) {
        LOGGER.info("Configuring injectorFileWriter with outputFile={}", outputFile);
        return new FlatFileItemWriterBuilder<CtrBlock>()
                .name("injectorFileWriter")
                .resource(new FileSystemResource(outputFile))
                .encoding("UTF-8")
                .shouldDeleteIfExists(true)
                .lineSeparator(System.lineSeparator())
                .lineAggregator(new CtrBlockLineAggregator(
                        new InjectorLineAggregator(), System.lineSeparator()))
                .build();
    }

    /**
     * Persists each contract (CTR header) to the {@code contract} table.
     *
     * <p>The SQL is externalised in {@code application.yml} under
     * {@code batch.injector.contract-upsert-sql} so it can be overridden
     * per profile: PostgreSQL uses {@code ON CONFLICT}, H2 (tests) uses
     * {@code MERGE INTO}.</p>
     *
     * <p>By the time a block reaches this writer the processor has already
     * filtered out orphan blocks and blocks with a missing contract ID,
     * so {@link CtrBlockToContractEntityConverter#convert} is guaranteed
     * to return a non-null entity.</p>
     */
    @Bean
    public JdbcBatchItemWriter<CtrBlock> injectorDbWriter(
            DataSource dataSource,
            @Value("${batch.injector.contract-upsert-sql}") String upsertSql) {
        LOGGER.info("Configuring injectorDbWriter for contract table");
        return new JdbcBatchItemWriterBuilder<CtrBlock>()
                .dataSource(dataSource)
                .sql(upsertSql)
                .itemPreparedStatementSetter((item, ps) -> {
                    ContractEntity entity = CtrBlockToContractEntityConverter.convert(item);
                    ps.setString(1, entity.contractId());
                    ps.setString(2, entity.clientId());
                    ps.setString(3, entity.startDate());
                    ps.setString(4, entity.status());
                    ps.setInt(5, entity.lineCount());
                })
                .build();
    }

    /**
     * Composite writer: delegates to both the file writer and the DB writer
     * in a single transaction per chunk.
     */
    @Bean
    @StepScope
    public CompositeItemWriter<CtrBlock> injectorCompositeWriter(
            FlatFileItemWriter<CtrBlock> injectorFileWriter,
            JdbcBatchItemWriter<CtrBlock> injectorDbWriter) {
        LOGGER.info("Configuring injectorCompositeWriter (file + database)");
        CompositeItemWriter<CtrBlock> compositeWriter = new CompositeItemWriter<>();
        compositeWriter.setDelegates(List.of(injectorFileWriter, injectorDbWriter));
        return compositeWriter;
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new InjectorJobExecutionListener();
    }

    @Bean
    public StepExecutionListener stepExecutionListener() {
        return new InjectorStepExecutionListener();
    }

    // -------------------------------------------------------------------------
    // Private named listeners — avoids anonymous-class bytecode overhead and
    // keeps each listener's logic isolated and independently testable.
    // -------------------------------------------------------------------------

    private static final class InjectorJobExecutionListener implements JobExecutionListener {

        @Override
        public void beforeJob(JobExecution jobExecution) {
            LOGGER.info("Job {} is starting with parameters: {}",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getJobParameters());
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            long durationMs = 0L;
            if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
                durationMs = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
            }
            if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                LOGGER.info("Job {} completed successfully in {} ms with status {}",
                        jobExecution.getJobInstance().getJobName(),
                        durationMs,
                        jobExecution.getStatus());
            } else {
                LOGGER.warn("Job {} finished with status {} in {} ms",
                        jobExecution.getJobInstance().getJobName(),
                        jobExecution.getStatus(),
                        durationMs);
            }
        }
    }

    private static final class InjectorStepExecutionListener implements StepExecutionListener {

        @Override
        public void beforeStep(StepExecution stepExecution) {
            LOGGER.info("Step {} is starting", stepExecution.getStepName());
        }

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            LOGGER.info("Step {} completed with status {} | blocksRead={} | blocksWritten={} | filterCount={} | skipCount={}",
                    stepExecution.getStepName(),
                    stepExecution.getStatus(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getFilterCount(),
                    stepExecution.getSkipCount());
            return stepExecution.getExitStatus();
        }
    }
}
