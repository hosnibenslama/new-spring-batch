package com.example.mdv02batch.injector.listener;

import com.example.mdv02batch.injector.dto.CtrBlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

/**
 * Listener that logs skipped CTR blocks during reading, processing, or writing.
 *
 * <p>Ensures that any contract block skipped due to errors (e.g. database constraints,
 * malformed format, or runtime exceptions) is recorded in logs for auditing without
 * interrupting the batch execution.</p>
 */
@Component
public class CtrBlockSkipListener implements SkipListener<CtrBlock, CtrBlock> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CtrBlockSkipListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        // item is not available when a read error occurs.
        LOGGER.warn("Skipped a contract block during reading: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(CtrBlock item, Throwable t) {
        // Spring Batch guarantees item is non-null here.
        LOGGER.warn("Skipped contract [{}] during processing: {}", item.reference(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(CtrBlock item, Throwable t) {
        // Log at ERROR with the full stack trace: a write failure is more serious
        // than a process failure and needs to be diagnosable from logs alone.
        LOGGER.error("Skipped contract [{}] during write", item.reference(), t);
    }
}
