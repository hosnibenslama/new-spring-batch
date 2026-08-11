package com.example.mdv02batch.injector;

import java.time.Duration;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.dto.CtrBlock;
import com.example.mdv02batch.injector.processor.CtrBlockItemProcessor;
import com.example.mdv02batch.injector.reader.CtrBlockItemReader;
import com.example.mdv02batch.injector.reader.InjectorBusinessDataLineMapper;
import com.example.mdv02batch.injector.writer.CtrBlockLineAggregator;
import com.example.mdv02batch.injector.writer.InjectorLineAggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JobInjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInjector.class);

    @Value("${batch.injector.chunk-size:1000}")
    private int chunkSize;

    @Value("${batch.injector.separator:;}")
    private String separator;

    @Value("${batch.injector.root-record-type:CTR}")
    private String rootRecordType;

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
     */
    @Bean
    public Step injectorStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             CtrBlockItemReader injectorBlockReader,
                             CtrBlockItemProcessor injectorProcessor,
                             FlatFileItemWriter<CtrBlock> injectorWriter) {
        return new StepBuilder("injectorStep", jobRepository)
                .<CtrBlock, CtrBlock>chunk(chunkSize, transactionManager)
                .listener(stepExecutionListener())
                .reader(injectorBlockReader)
                .processor(injectorProcessor)
                .writer(injectorWriter)
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
    public FlatFileItemWriter<CtrBlock> injectorWriter(
            @Value("#{jobParameters['outputFile'] ?: '${batch.injector.output-file}'}") String outputFile) {
        LOGGER.info("Configuring injectorWriter with outputFile={}", outputFile);
        return new FlatFileItemWriterBuilder<CtrBlock>()
                .name("injectorWriter")
                .resource(new FileSystemResource(outputFile))
                .encoding("UTF-8")
                .shouldDeleteIfExists(true)
                .lineSeparator(System.lineSeparator())
                .lineAggregator(new CtrBlockLineAggregator(
                        new InjectorLineAggregator(), System.lineSeparator()))
                .build();
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
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
        };
    }

    @Bean
    public StepExecutionListener stepExecutionListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                LOGGER.info("Step {} is starting", stepExecution.getStepName());
            }

            @Override
            public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
                LOGGER.info("Step {} completed with status {} | blocksRead={} | blocksWritten={} | filterCount={} | skipCount={}",
                        stepExecution.getStepName(),
                        stepExecution.getStatus(),
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getFilterCount(),
                        stepExecution.getSkipCount());
                return stepExecution.getExitStatus();
            }
        };
    }
}
