package com.example.mdv02batch.injector.writer;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import org.springframework.batch.item.file.transform.LineAggregator;

/**
 * Writes the original raw line back to the output file.
 */
public class InjectorLineAggregator implements LineAggregator<BusinessDataLine> {

    @Override
    public String aggregate(BusinessDataLine item) {
        return item.rawLine();
    }
}
