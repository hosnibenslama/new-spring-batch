package com.example.mdv02batch.injector;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.processor.InjectorItemProcessor;
import com.example.mdv02batch.injector.reader.InjectorBusinessDataLineMapper;
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

import java.time.Duration;

@Configuration
public class JobInjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInjector.class);

    @Value("${batch.injector.chunk-size:1000}")
    private int chunkSize;

    @Value("${batch.injector.separator:;}")
    private String separator;

    @Bean
    public Job injectorJob(JobRepository jobRepository, Step injectorStep) {
        return new JobBuilder("injectorJob", jobRepository)
                .listener(jobExecutionListener())
                .start(injectorStep)
                .build();
    }

    @Bean
    public Step injectorStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             FlatFileItemReader<BusinessDataLine> injectorReader,
                             InjectorItemProcessor injectorProcessor,
                             FlatFileItemWriter<BusinessDataLine> injectorWriter) {
        return new StepBuilder("injectorStep", jobRepository)
                .<BusinessDataLine, BusinessDataLine>chunk(chunkSize, transactionManager)
                .listener(stepExecutionListener())
                .reader(injectorReader)
                .processor(injectorProcessor)
                .writer(injectorWriter)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<BusinessDataLine> injectorReader(
            @Value("#{jobParameters['inputFile'] ?: '${batch.injector.input-file}'}") Resource inputFile) {
        LOGGER.info("Configuring injectorReader with inputFile={}", inputFile);
        return new FlatFileItemReaderBuilder<BusinessDataLine>()
                .name("injectorReader")
                .resource(inputFile)
                .encoding("UTF-8")
                .lineMapper(new InjectorBusinessDataLineMapper(separator))
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<BusinessDataLine> injectorWriter(
            @Value("#{jobParameters['outputFile'] ?: '${batch.injector.output-file}'}") String outputFile) {
        LOGGER.info("Configuring injectorWriter with outputFile={}", outputFile);
        return new FlatFileItemWriterBuilder<BusinessDataLine>()
                .name("injectorWriter")
                .resource(new FileSystemResource(outputFile))
                .encoding("UTF-8")
                .shouldDeleteIfExists(true)
                .lineAggregator(new InjectorLineAggregator())
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
                LOGGER.info("Step {} completed with status {} | readCount={} | writeCount={} | skipCount={}",
                        stepExecution.getStepName(),
                        stepExecution.getStatus(),
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getSkipCount());
                return stepExecution.getExitStatus();
            }
        };
    }
}
