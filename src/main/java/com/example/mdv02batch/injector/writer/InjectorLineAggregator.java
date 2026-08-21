package com.example.mdv02batch.injector.writer;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import org.springframework.batch.infrastructure.item.file.transform.LineAggregator;

/**
 * Writes the original raw line back to the output file.
 *
 * <p>No longer the aggregator of the step: since the item is a
 * {@link com.example.mdv02batch.injector.dto.CtrBlock}, this class is used as
 * the per-line delegate of {@link CtrBlockLineAggregator}. Keeping it isolated
 * means the way a single line is rendered can evolve (re-serialization from the
 * parsed fields, for instance) without touching the block assembly.</p>
 */
public class InjectorLineAggregator implements LineAggregator<BusinessDataLine> {

    @Override
    public String aggregate(BusinessDataLine item) {
        return item.rawLine();
    }
}
