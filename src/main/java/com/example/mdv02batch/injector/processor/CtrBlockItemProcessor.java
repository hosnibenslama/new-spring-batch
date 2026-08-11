package com.example.mdv02batch.injector.processor;

import com.example.mdv02batch.injector.dto.CtrBlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Block-scoped processor for the injector technical foundation.
 *
 * <p>No transformation is applied yet: the block is forwarded unchanged. This is
 * the extension point where the CTR compliance checks will plug in, returning
 * {@code null} to filter a non-compliant block out of the nominal flow.</p>
 */
@Component
public class CtrBlockItemProcessor implements ItemProcessor<CtrBlock, CtrBlock> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CtrBlockItemProcessor.class);

    @Override
    public CtrBlock process(CtrBlock item) {
        LOGGER.debug("Processing {}", item.reference());
        return item;
    }
}
