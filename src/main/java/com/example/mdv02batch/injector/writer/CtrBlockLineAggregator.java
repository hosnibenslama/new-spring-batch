package com.example.mdv02batch.injector.writer;

import java.util.List;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.dto.CtrBlock;

import org.springframework.batch.infrastructure.item.file.transform.LineAggregator;
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
 * <p>Uses a {@code StringBuilder} instead of {@code Collectors.joining()} to
 * avoid intermediate {@code String[]} and {@code StringJoiner} allocations on
 * each of the millions of aggregations.</p>
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
        List<BusinessDataLine> lines = item.lines();
        StringBuilder sb = new StringBuilder(lines.size() * 64);
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append(this.lineSeparator);
            }
            sb.append(this.lineAggregator.aggregate(lines.get(i)));
        }
        return sb.toString();
    }
}
