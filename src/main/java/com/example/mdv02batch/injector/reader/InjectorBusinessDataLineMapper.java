package com.example.mdv02batch.injector.reader;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import org.springframework.batch.item.file.LineMapper;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Maps one physical input line to one BusinessDataLine.
 */
public class InjectorBusinessDataLineMapper implements LineMapper<BusinessDataLine> {

    private final String separator;

    public InjectorBusinessDataLineMapper(String separator) {
        this.separator = separator;
    }

    @Override
    public BusinessDataLine mapLine(String line, int lineNumber) {
        int indentation = countLeadingWhitespace(line);
        String trimmedLine = line.stripLeading();
        String[] split = trimmedLine.split(Pattern.quote(separator), -1);
        List<String> fields = Arrays.asList(split);
        String recordType = fields.isEmpty() ? "" : fields.get(0);

        return new BusinessDataLine(line, lineNumber, indentation, recordType, fields);
    }

    private int countLeadingWhitespace(String value) {
        int count = 0;
        while (count < value.length() && Character.isWhitespace(value.charAt(count))) {
            count++;
        }
        return count;
    }
}
