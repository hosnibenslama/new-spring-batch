package com.example.mdv02batch.injector.processor;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Minimal processor for the injector technical foundation.
 * No transformation is applied.
 */
@Component
public class InjectorItemProcessor implements ItemProcessor<BusinessDataLine, BusinessDataLine> {

    @Override
    public BusinessDataLine process(BusinessDataLine item) {
        return item;
    }
}
