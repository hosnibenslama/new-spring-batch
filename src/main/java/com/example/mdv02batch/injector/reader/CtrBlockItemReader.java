package com.example.mdv02batch.injector.reader;

import java.util.ArrayList;
import java.util.List;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.dto.CtrBlock;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.util.Assert;

/**
 * Groups the physical lines produced by the flat file reader into
 * {@link CtrBlock} items.
 *
 * <p>Grouping rule: a new block starts on every line whose record type is the
 * configured root type ({@code CTR}). Every following line belongs to the
 * current block until the next root line is peeked. The one-line lookahead is
 * delegated to {@link SingleItemPeekableItemReader}, whose peek buffer is saved
 * in the {@link ExecutionContext} so restart semantics are preserved.</p>
 *
 * <p>Indentation is not used as the grouping criterion on purpose: it is
 * cosmetic in the input file and a single mis-indented line would silently
 * reshape the tree. The record type is the contract.</p>
 */
public class CtrBlockItemReader implements ItemStreamReader<CtrBlock> {

    public static final String DEFAULT_ROOT_RECORD_TYPE = "CTR";

    private final SingleItemPeekableItemReader<BusinessDataLine> delegate;

    private final String rootRecordType;

    public CtrBlockItemReader(ItemReader<BusinessDataLine> lineReader) {
        this(lineReader, DEFAULT_ROOT_RECORD_TYPE);
    }

    public CtrBlockItemReader(ItemReader<BusinessDataLine> lineReader, String rootRecordType) {
        Assert.notNull(lineReader, "lineReader must not be null");
        Assert.hasText(rootRecordType, "rootRecordType must not be empty");
        SingleItemPeekableItemReader<BusinessDataLine> peekable = new SingleItemPeekableItemReader<>();
        peekable.setDelegate(lineReader);
        this.delegate = peekable;
        this.rootRecordType = rootRecordType;
    }

    @Override
    public CtrBlock read() throws Exception {
        BusinessDataLine firstLine = this.delegate.read();
        if (firstLine == null) {
            return null;
        }

        if (!isRootLine(firstLine)) {
            List<BusinessDataLine> orphanLines = new ArrayList<>();
            orphanLines.add(firstLine);
            collectUntilNextRootLine(orphanLines);
            return CtrBlock.orphan(orphanLines);
        }

        List<BusinessDataLine> children = new ArrayList<>();
        collectUntilNextRootLine(children);
        return CtrBlock.of(firstLine, children);
    }

    private void collectUntilNextRootLine(List<BusinessDataLine> target) throws Exception {
        while (true) {
            BusinessDataLine nextLine = this.delegate.peek();
            if (nextLine == null || isRootLine(nextLine)) {
                return;
            }
            target.add(this.delegate.read());
        }
    }

    private boolean isRootLine(BusinessDataLine line) {
        return this.rootRecordType.equals(line.recordType());
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        this.delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        this.delegate.close();
    }
}
