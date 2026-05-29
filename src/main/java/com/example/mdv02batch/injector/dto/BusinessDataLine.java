package com.example.mdv02batch.injector.dto;

import java.util.List;

/**
 * Represents one business data update read from the input file.
 *
 * Each physical line in the file corresponds to one business update.
 * Indentation may be present and is preserved as metadata.
 * Fields are separated by ';'.
 */
public record BusinessDataLine(
        String rawLine,
        int lineNumber,
        int indentationLevel,
        String recordType,
        List<String> fields
) {

    public String primaryIdentifier() {
        return fields.size() > 1 ? fields.get(1) : null;
    }
}
