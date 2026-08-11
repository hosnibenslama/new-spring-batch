package com.example.mdv02batch.injector.unit;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.writer.InjectorLineAggregator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InjectorLineAggregatorUnitTest {

    private final InjectorLineAggregator aggregator = new InjectorLineAggregator();

    @Test
    void shouldReturnRawLineExactlyAsReceived() {
        BusinessDataLine line = new BusinessDataLine(
                "  OM;OM_001;CTR_123456;BASE_OFFER",
                2,
                2,
                "OM",
                List.of("OM", "OM_001", "CTR_123456", "BASE_OFFER")
        );

        String aggregated = aggregator.aggregate(line);

        assertThat(aggregated).isEqualTo("  OM;OM_001;CTR_123456;BASE_OFFER");
    }
}
