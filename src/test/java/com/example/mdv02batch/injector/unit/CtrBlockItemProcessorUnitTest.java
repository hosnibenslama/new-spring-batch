package com.example.mdv02batch.injector.unit;

import java.util.List;

import com.example.mdv02batch.injector.dto.CtrBlock;
import com.example.mdv02batch.injector.processor.CtrBlockItemProcessor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CtrBlockItemProcessor")
class CtrBlockItemProcessorUnitTest {

    // A new processor instance is created per test by JUnit 5 default lifecycle,
    // so the AtomicLong counter always starts at zero in each test.
    private final CtrBlockItemProcessor processor = new CtrBlockItemProcessor();

    @Test
    @DisplayName("forwards a valid contract block unchanged")
    void shouldForwardValidBlockUnchanged() {
        CtrBlock block = CtrBlock.of(
                CtrBlockFixtures.line("CTR;123456;CLIENT_001;20240101;ACTIVE", 1),
                List.of(CtrBlockFixtures.line("  OM;OM_001;CTR_123456;BASE_OFFER", 2)));

        assertThat(this.processor.process(block)).isSameAs(block);
    }

    @Test
    @DisplayName("filters out orphan blocks (lines before any CTR header)")
    void shouldFilterOutOrphanBlock() {
        CtrBlock orphan = CtrBlock.orphan(
                List.of(CtrBlockFixtures.line("  OM;OM_001;CTR_123456;BASE_OFFER", 1)));

        assertThat(this.processor.process(orphan)).isNull();
    }

    @Test
    @DisplayName("filters out a block whose contract identifier is empty")
    void shouldFilterOutBlockWithEmptyContractId() {
        CtrBlock invalid = CtrBlock.of(
                CtrBlockFixtures.line("CTR;;CLIENT_001;20240101;ACTIVE", 1),
                List.of());

        assertThat(this.processor.process(invalid)).isNull();
    }

    @Test
    @DisplayName("filters out a block whose contract identifier is blank (whitespace only)")
    void shouldFilterOutBlockWithBlankContractId() {
        CtrBlock invalid = CtrBlock.of(
                CtrBlockFixtures.line("CTR;   ;CLIENT_001;20240101;ACTIVE", 1),
                List.of());

        assertThat(this.processor.process(invalid)).isNull();
    }
}
