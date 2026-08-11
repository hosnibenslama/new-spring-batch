package com.example.mdv02batch.injector.unit;

import java.util.List;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.dto.CtrBlock;
import com.example.mdv02batch.injector.writer.CtrBlockLineAggregator;
import com.example.mdv02batch.injector.writer.InjectorLineAggregator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CtrBlockLineAggregatorUnitTest {

    private static final String SEPARATOR = "\n";

    private final CtrBlockLineAggregator aggregator =
            new CtrBlockLineAggregator(new InjectorLineAggregator(), SEPARATOR);

    @Test
    void shouldRenderHeaderAndChildrenInFileOrderPreservingIndentation() {
        List<BusinessDataLine> lines = CtrBlockFixtures.asLines(List.of(
                "CTR;123456;CLIENT_001;20240101;ACTIVE",
                "  OM;OM_001;CTR_123456;BASE_OFFER",
                "    ART;ART_001;OM_001;INTERNET_SERVICE;100.00;EUR"));
        CtrBlock block = CtrBlock.of(lines.get(0), lines.subList(1, 3));

        String aggregated = this.aggregator.aggregate(block);

        assertThat(aggregated).isEqualTo(
                "CTR;123456;CLIENT_001;20240101;ACTIVE\n"
                        + "  OM;OM_001;CTR_123456;BASE_OFFER\n"
                        + "    ART;ART_001;OM_001;INTERNET_SERVICE;100.00;EUR");
    }

    @Test
    void shouldNotAppendSeparatorAfterTheLastLine() {
        List<BusinessDataLine> lines = CtrBlockFixtures.asLines(List.of(
                "CTR;123456;CLIENT_001;20240101;ACTIVE",
                "  OM;OM_001;CTR_123456;BASE_OFFER"));
        CtrBlock block = CtrBlock.of(lines.get(0), lines.subList(1, 2));

        assertThat(this.aggregator.aggregate(block)).doesNotEndWith(SEPARATOR);
    }

    @Test
    void shouldRenderASingleLineBlockAsThatLine() {
        BusinessDataLine header = CtrBlockFixtures.line("CTR;123456;CLIENT_001;20240101;ACTIVE", 1);
        CtrBlock block = CtrBlock.of(header, List.of());

        assertThat(this.aggregator.aggregate(block))
                .isEqualTo("CTR;123456;CLIENT_001;20240101;ACTIVE");
    }

    @Test
    void shouldRenderAnOrphanBlockWithoutLosingAnyLine() {
        List<BusinessDataLine> lines = CtrBlockFixtures.asLines(List.of(
                "  OM;OM_999;CTR_UNKNOWN;BASE_OFFER",
                "    ART;ART_999;OM_999;INTERNET_SERVICE;10.00;EUR"));
        CtrBlock block = CtrBlock.orphan(lines);

        assertThat(block.orphan()).isTrue();
        assertThat(this.aggregator.aggregate(block)).isEqualTo(
                "  OM;OM_999;CTR_UNKNOWN;BASE_OFFER\n"
                        + "    ART;ART_999;OM_999;INTERNET_SERVICE;10.00;EUR");
    }

    @Test
    void shouldUseTheConfiguredLineSeparator()  {
        List<BusinessDataLine> lines = CtrBlockFixtures.asLines(List.of(
                "CTR;123456;CLIENT_001;20240101;ACTIVE",
                "  OM;OM_001;CTR_123456;BASE_OFFER"));
        CtrBlock block = CtrBlock.of(lines.get(0), lines.subList(1, 2));

        CtrBlockLineAggregator crlfAggregator =
                new CtrBlockLineAggregator(new InjectorLineAggregator(), "\r\n");

        assertThat(crlfAggregator.aggregate(block)).contains("ACTIVE\r\n  OM;");
    }

    @Test
    void shouldRejectANullItem() {
        assertThatThrownBy(() -> this.aggregator.aggregate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
