package com.example.mdv02batch.injector.unit;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.reader.InjectorBusinessDataLineMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InjectorBusinessDataLineMapperUnitTest {

    private final InjectorBusinessDataLineMapper mapper = new InjectorBusinessDataLineMapper(";");

    @Test
    void shouldMapSemicolonSeparatedLineWithIndentation() {
        BusinessDataLine line = mapper.mapLine("    ART;ART_001;OM_001;INTERNET_SERVICE;100.00;EUR", 3);

        assertThat(line.lineNumber()).isEqualTo(3);
        assertThat(line.indentationLevel()).isEqualTo(4);
        assertThat(line.recordType()).isEqualTo("ART");
        assertThat(line.fields()).containsExactly("ART", "ART_001", "OM_001", "INTERNET_SERVICE", "100.00", "EUR");
        assertThat(line.primaryIdentifier()).isEqualTo("ART_001");
        assertThat(line.rawLine()).isEqualTo("    ART;ART_001;OM_001;INTERNET_SERVICE;100.00;EUR");
    }

    @Test
    void shouldKeepEmptyTrailingFields() {
        BusinessDataLine line = mapper.mapLine("COND;COND_001;CTR_123456;", 7);

        assertThat(line.fields()).containsExactly("COND", "COND_001", "CTR_123456", "");
        assertThat(line.recordType()).isEqualTo("COND");
    }
}
