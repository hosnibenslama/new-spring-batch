package com.example.mdv02batch.injector.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One CTR contract block: a {@code CTR} header line plus every subordinate line
 * that belongs to it ({@code OM}, {@code ART}, {@code OFF}, {@code ACC},
 * {@code ROL}, {@code COND}, {@code OID}, ...).
 *
 * <p>The block, not the physical line, is now the batch item. Reading and
 * writing the whole block keeps a contract atomic inside a chunk: compliance
 * checks, rejection and injection all apply to a complete contract instead of
 * an isolated line.</p>
 *
 * <p>A block whose {@code header} is {@code null} is an <b>orphan block</b>:
 * lines read before any {@code CTR} header (malformed file, or leading blank
 * lines). They are carried as-is so the output stays byte-for-byte comparable
 * with the input, and can be rejected later without losing data.</p>
 *
 * @param startLineNumber physical line number of the first line of the block
 * @param header          the {@code CTR} line, {@code null} for an orphan block
 * @param children        subordinate lines, in file order
 */
public record CtrBlock(
        int startLineNumber,
        BusinessDataLine header,
        List<BusinessDataLine> children) {

    public CtrBlock {
        // Wrap in an unmodifiable view instead of List.copyOf to avoid
        // the element-by-element null check copy on every contract (5M+).
        children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public static CtrBlock of(BusinessDataLine header, List<BusinessDataLine> children) {
        return new CtrBlock(header.lineNumber(), header, children);
    }

    public static CtrBlock orphan(List<BusinessDataLine> lines) {
        int startLineNumber = lines.isEmpty() ? 0 : lines.get(0).lineNumber();
        return new CtrBlock(startLineNumber, null, lines);
    }

    public boolean orphan() {
        return this.header == null;
    }

    /** Contract identifier carried by the CTR header, {@code null} for an orphan block. */
    public String contractId() {
        return this.header == null ? null : this.header.primaryIdentifier();
    }

    /**
     * Every line of the block, header first, in file order.
     * Returns a new list each call — callers may iterate but not cache.
     */
    public List<BusinessDataLine> lines() {
        if (this.header == null) {
            return this.children;
        }
        List<BusinessDataLine> all = new ArrayList<>(this.children.size() + 1);
        all.add(this.header);
        all.addAll(this.children);
        return all;
    }

    public int lineCount() {
        return this.children.size() + (this.header == null ? 0 : 1);
    }

    public List<BusinessDataLine> linesOfType(String recordType) {
        return lines().stream()
                .filter(line -> recordType.equals(line.recordType()))
                .toList();
    }

    public Optional<BusinessDataLine> firstOfType(String recordType) {
        return lines().stream()
                .filter(line -> recordType.equals(line.recordType()))
                .findFirst();
    }

    /** Short label for logs and future rejection files. */
    public String reference() {
        return this.orphan()
                ? "orphanBlock[startLine=%d, lines=%d]".formatted(this.startLineNumber, lineCount())
                : "ctrBlock[startLine=%d, contract=%s, lines=%d]"
                        .formatted(this.startLineNumber, contractId(), lineCount());
    }
}

