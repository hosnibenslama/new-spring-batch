package com.example.mdv02batch.injector.unit;

import java.util.List;

import com.example.mdv02batch.injector.dto.CtrBlock;
import com.example.mdv02batch.injector.processor.CtrBlockItemProcessor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CtrBlockItemProcessorUnitTest {

    private final CtrBlockItemProcessor processor = new CtrBlockItemProcessor();

    @Test
    void shouldForwardTheBlockUnchanged() {
        CtrBlock block = CtrBlock.of(
                CtrBlockFixtures.line("CTR;123456;CLIENT_001;20240101;ACTIVE", 1),
                List.of(CtrBlockFixtures.line("  OM;OM_001;CTR_123456;BASE_OFFER", 2)));

        assertThat(this.processor.process(block)).isSameAs(block);
    }
}
