package com.example.mdv02batch.injector.writer;

import java.util.stream.Collectors;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.dto.CtrBlock;

import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.util.Assert;

/**
 * Serializes a whole {@link CtrBlock} into the text written by the
 * {@code FlatFileItemWriter}.
 *
 * <p>Replaces the line-scoped {@code InjectorLineAggregator} as the aggregator
 * of the step: one item is now one contract block, so the aggregated value is a
 * multi-line string. Each line is delegated to a
 * {@code LineAggregator<BusinessDataLine>} — {@link InjectorLineAggregator} by
 * default — and the results are joined with the block line separator.</p>
 *
 * <p>The writer appends its own line separator after each item, so the
 * separator used here must not be appended after the last line of the block,
 * otherwise every block would be followed by a blank line.</p>
 */
public class CtrBlockLineAggregator implements LineAggregator<CtrBlock> {

    private final LineAggregator<BusinessDataLine> lineAggregator;

    private final String lineSeparator;

    public CtrBlockLineAggregator() {
        this(new InjectorLineAggregator(), System.lineSeparator());
    }

    public CtrBlockLineAggregator(LineAggregator<BusinessDataLine> lineAggregator,
            String lineSeparator) {
        Assert.notNull(lineAggregator, "lineAggregator must not be null");
        Assert.notNull(lineSeparator, "lineSeparator must not be null");
        this.lineAggregator = lineAggregator;
        this.lineSeparator = lineSeparator;
    }

    @Override
    public String aggregate(CtrBlock item) {
        Assert.notNull(item, "item must not be null");
        return item.lines().stream()
                .map(this.lineAggregator::aggregate)
                .collect(Collectors.joining(this.lineSeparator));
    }
}
