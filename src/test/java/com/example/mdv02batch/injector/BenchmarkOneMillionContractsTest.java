package com.example.mdv02batch.injector;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BenchmarkOneMillionContractsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkOneMillionContractsTest.class);
    private static final int CONTRACT_COUNT = 1_000_000;
    private static final Path DATASET_PATH = Paths.get("target", "contracts_1M_input.txt");
    private static final Path OUTPUT_PATH = Paths.get("target", "contracts_1M_output.txt");

    private final JobOperator jobOperator;
    private final Job injectorJob;

    @Autowired
    public BenchmarkOneMillionContractsTest(
            JobOperator jobOperator,
            @Qualifier("injectorJob") Job injectorJob) {
        this.jobOperator = jobOperator;
        this.injectorJob = injectorJob;
    }

    @Test
    @DisplayName("Benchmark processing 1,000,000 contracts through the Spring Batch pipeline")
    void benchmarkOneMillionContractsExecutionTime() throws Exception {
        // 1. Ensure 1M input dataset exists
        generateDatasetIfMissing(DATASET_PATH, CONTRACT_COUNT);

        Files.deleteIfExists(OUTPUT_PATH);

        LOGGER.info("===============================================================");
        LOGGER.info("STARTING BENCHMARK: 1,000,000 CONTRACTS (~8M PHYSICAL LINES)");
        LOGGER.info("Input file:  {}", DATASET_PATH.toAbsolutePath());
        LOGGER.info("Output file: {}", OUTPUT_PATH.toAbsolutePath());
        LOGGER.info("===============================================================");

        JobParameters params = new JobParametersBuilder()
                .addString("runDate", "benchmark_1M_" + System.currentTimeMillis(), true)
                .addString("inputFile", DATASET_PATH.toUri().toString(), false)
                .addString("outputFile", OUTPUT_PATH.toAbsolutePath().toString(), false)
                .toJobParameters();

        Instant start = Instant.now();
        JobExecution execution = this.jobOperator.start(this.injectorJob, params);
        Instant end = Instant.now();

        Duration duration = Duration.between(start, end);
        long totalMs = duration.toMillis();
        double seconds = totalMs / 1000.0;
        double contractsPerSec = seconds > 0 ? (CONTRACT_COUNT / seconds) : 0;
        double linesPerSec = seconds > 0 ? ((CONTRACT_COUNT * 8.0) / seconds) : 0;

        StepExecution stepExecution = execution.getStepExecutions().iterator().next();

        LOGGER.info("===============================================================");
        LOGGER.info("BENCHMARK COMPLETED in {} ms ({:.2f} seconds)", totalMs, seconds);
        LOGGER.info("Status:              {}", execution.getStatus());
        LOGGER.info("Blocks Read:         {}", stepExecution.getReadCount());
        LOGGER.info("Blocks Written:      {}", stepExecution.getWriteCount());
        LOGGER.info("Blocks Filtered:     {}", stepExecution.getFilterCount());
        LOGGER.info("Blocks Skipped:      {}", stepExecution.getSkipCount());
        LOGGER.info("Contract Throughput: {:.0f} contracts/second", contractsPerSec);
        LOGGER.info("Line Throughput:     {:.0f} lines/second", linesPerSec);
        LOGGER.info("Output File Size:    {:.2f} MB", Files.size(OUTPUT_PATH) / (1024.0 * 1024.0));
        LOGGER.info("===============================================================");

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getReadCount()).isEqualTo(CONTRACT_COUNT);
        assertThat(stepExecution.getWriteCount()).isEqualTo(CONTRACT_COUNT);
        assertThat(OUTPUT_PATH).exists();
    }

    private static void generateDatasetIfMissing(Path path, int count) throws Exception {
        if (Files.exists(path) && Files.size(path) > 0) {
            LOGGER.info("Found existing 1M dataset at {} ({:.2f} MB)",
                    path, Files.size(path) / (1024.0 * 1024.0));
            return;
        }

        Files.createDirectories(path.getParent());
        LOGGER.info("Generating {} contracts into {} ...", count, path);
        Instant start = Instant.now();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()), 64 * 1024)) {
            for (int i = 1; i <= count; i++) {
                String ctrId = String.format("%07d", i);
                String clientId = String.format("CLIENT_%06d", (i % 50_000) + 1);

                writer.write("CTR;" + ctrId + ";" + clientId + ";20240101;ACTIVE\n");
                writer.write("  OM;OM_" + ctrId + ";CTR_" + ctrId + ";BASE_OFFER\n");
                writer.write("    ART;ART_" + ctrId + ";OM_" + ctrId + ";INTERNET_SERVICE;100.00;EUR\n");
                writer.write("      OFF;OFF_" + ctrId + ";ART_" + ctrId + ";FIBER_OFFER;2024-01-01;2025-01-01\n");
                writer.write("      ACC;ACC_" + ctrId + ";OFF_" + ctrId + ";MAIN_ACCESS\n");
                writer.write("        ROL;ROL_" + ctrId + ";ACC_" + ctrId + ";HOLDER;" + clientId + "\n");
                writer.write("  COND;COND_" + ctrId + ";CTR_" + ctrId + ";COMMITMENT_12M;2024-01-01;2025-01-01\n");
                writer.write("  OID;OID_" + ctrId + ";CTR_" + ctrId + ";EXTERNAL_IDENTIFIER;EXT_" + ctrId + "\n");

                if (i % 200_000 == 0) {
                    LOGGER.info("Generated {} / {} contracts...", i, count);
                }
            }
        }

        Duration duration = Duration.between(start, Instant.now());
        LOGGER.info("Dataset generation completed in {:.2f} s ({:.2f} MB)",
                duration.toMillis() / 1000.0, Files.size(path) / (1024.0 * 1024.0));
    }
}
