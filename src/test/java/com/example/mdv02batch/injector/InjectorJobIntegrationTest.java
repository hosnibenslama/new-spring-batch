package com.example.mdv02batch.injector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.reader.InjectorBusinessDataLineMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
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
class InjectorJobIntegrationTest {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    @Qualifier("injectorJob")
    private Job injectorJob;

    @TempDir
    Path tempDir;

    @Test
    void shouldCompleteSuccessfully() throws Exception {
        Path outputFile = this.tempDir.resolve("contracts_output.txt");

        // Spring Batch 6: addString defaults to identifying=false; pass true explicitly.
        var params = new JobParametersBuilder()
                .addString("runDate", LocalDateTime.now() + "_test1", true)
                .addString("inputFile", "classpath:input/contracts_input.txt", false)
                .addString("outputFile", outputFile.toString(), false)
                .toJobParameters();

        var execution = this.jobOperator.start(this.injectorJob, params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(outputFile).exists();
    }

    @Test
    void outputFileShouldContainExactSameLinesAsInput() throws Exception {
        Path outputFile = this.tempDir.resolve("contracts_output_ca6.txt");

        var params = new JobParametersBuilder()
                .addString("runDate", LocalDateTime.now() + "_test2", true)
                .addString("inputFile", "classpath:input/contracts_input.txt", false)
                .addString("outputFile", outputFile.toString(), false)
                .toJobParameters();

        List<String> inputLines = readInputLines();

        this.jobOperator.start(this.injectorJob, params);

        List<String> outputLines = Files.readAllLines(outputFile)
                .stream()
                .filter(line -> !line.isBlank())
                .toList();

        assertThat(outputLines).containsExactlyElementsOf(inputLines);
    }

    /**
     * The step now counts contract blocks: the read and write counts must match
     * the number of CTR header lines, not the number of physical lines.
     */
    @Test
    void stepShouldCountBlocksAndNotPhysicalLines() throws Exception {
        Path outputFile = this.tempDir.resolve("contracts_output_blocks.txt");

        var params = new JobParametersBuilder()
                .addString("runDate", LocalDateTime.now() + "_test3", true)
                .addString("inputFile", "classpath:input/contracts_input.txt", false)
                .addString("outputFile", outputFile.toString(), false)
                .toJobParameters();

        List<String> inputLines = readInputLines();
        long expectedBlocks = inputLines.stream().filter(line -> line.startsWith("CTR;")).count();

        var execution = this.jobOperator.start(this.injectorJob, params);
        StepExecution stepExecution = execution.getStepExecutions().iterator().next();

        assertThat(expectedBlocks).isGreaterThan(0);
        assertThat(stepExecution.getReadCount()).isEqualTo(expectedBlocks);
        assertThat(stepExecution.getWriteCount()).isEqualTo(expectedBlocks);
        assertThat(stepExecution.getFilterCount()).isZero();
    }

    @Test
    void mapperShouldParseSemicolonAndIndentation() {
        InjectorBusinessDataLineMapper mapper = new InjectorBusinessDataLineMapper(";");

        BusinessDataLine line = mapper.mapLine("    ART;ART_001;OM_001;INTERNET_SERVICE;100.00;EUR", 3);

        assertThat(line.lineNumber()).isEqualTo(3);
        assertThat(line.indentationLevel()).isEqualTo(4);
        assertThat(line.recordType()).isEqualTo("ART");
        assertThat(line.fields()).containsExactly("ART", "ART_001", "OM_001", "INTERNET_SERVICE", "100.00", "EUR");
        assertThat(line.primaryIdentifier()).isEqualTo("ART_001");
        assertThat(line.rawLine()).isEqualTo("    ART;ART_001;OM_001;INTERNET_SERVICE;100.00;EUR");
    }

    private List<String> readInputLines() throws Exception {
        var inputStream = getClass().getClassLoader().getResourceAsStream("input/contracts_input.txt");
        assertThat(inputStream).isNotNull();
        return new String(inputStream.readAllBytes())
                .lines()
                .filter(line -> !line.isBlank())
                .toList();
    }
}
