package com.example.mdv02batch.injector.reader;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import org.springframework.batch.item.file.LineMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps one physical input line to one BusinessDataLine.
 *
 * <p>Uses a fast {@code indexOf}-based splitter instead of regex to avoid
 * compiling a {@code Pattern} on every one of the millions of input lines.</p>
 */
public class InjectorBusinessDataLineMapper implements LineMapper<BusinessDataLine> {

    private final char separatorChar;

    public InjectorBusinessDataLineMapper(String separator) {
        if (separator == null || separator.isEmpty()) {
            throw new IllegalArgumentException("separator must not be null or empty");
        }
        this.separatorChar = separator.charAt(0);
    }

    @Override
    public BusinessDataLine mapLine(String line, int lineNumber) {
        int indentation = countLeadingWhitespace(line);
        String trimmedLine = line.stripLeading();
        List<String> fields = splitFast(trimmedLine);
        String recordType = fields.isEmpty() ? "" : fields.get(0);

        return new BusinessDataLine(line, lineNumber, indentation, recordType, fields);
    }

    /**
     * Index-based splitting — no regex, no Pattern compilation.
     * Preserves empty trailing fields (equivalent to {@code split(sep, -1)}).
     */
    private List<String> splitFast(String line) {
        List<String> result = new ArrayList<>(8);
        int start = 0;
        int idx;
        while ((idx = line.indexOf(separatorChar, start)) >= 0) {
            result.add(line.substring(start, idx));
            start = idx + 1;
        }
        result.add(line.substring(start));
        return result;
    }

    private int countLeadingWhitespace(String value) {
        int count = 0;
        while (count < value.length() && Character.isWhitespace(value.charAt(count))) {
            count++;
        }
        return count;
    }
}
