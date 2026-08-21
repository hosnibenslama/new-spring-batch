package com.example.mdv02batch.injector.processor;

import java.util.concurrent.atomic.AtomicLong;

import com.example.mdv02batch.injector.dto.CtrBlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Block-scoped processor for the injector technical foundation.
 *
 * <p>Filters out orphan blocks (lines appearing before any CTR header) and
 * blocks with a missing contract identifier by returning {@code null}.
 * Returning {@code null} causes Spring Batch to increment
 * {@code StepExecution.filterCount} and skip the write phase for that block
 * without counting it as a skip or an error.</p>
 *
 * <p>Logs progress every {@value #LOG_INTERVAL} valid contracts at INFO level.
 * Business compliance checks should be added here and should return
 * {@code null} or throw a skippable exception for non-compliant blocks.</p>
 */
@StepScope
@Component
public class CtrBlockItemProcessor implements ItemProcessor<CtrBlock, CtrBlock> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CtrBlockItemProcessor.class);

    private static final long LOG_INTERVAL = 10_000L;

    private final AtomicLong counter = new AtomicLong();

    @Override
    public CtrBlock process(CtrBlock item) {
        // Spring Batch never passes null; guard is omitted intentionally.
        if (item.orphan()) {
            LOGGER.warn("Filtering out orphan block at line {}", item.startLineNumber());
            return null;
        }

        // Cache to avoid calling the method twice for the same item.
        String contractId = item.contractId();
        if (contractId == null || contractId.isBlank()) {
            LOGGER.warn("Filtering out block at line {}: missing contract identifier",
                    item.startLineNumber());
            return null;
        }

        long count = counter.incrementAndGet();
        if (count % LOG_INTERVAL == 0) {
            LOGGER.info("Processed {} contracts so far", count);
        }
        return item;
    }
}
