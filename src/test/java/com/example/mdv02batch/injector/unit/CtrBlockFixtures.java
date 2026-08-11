package com.example.mdv02batch.injector.unit;

import java.util.List;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.reader.InjectorBusinessDataLineMapper;

/** Test fixtures: builds {@link BusinessDataLine} instances from raw text lines. */
final class CtrBlockFixtures {

    static final InjectorBusinessDataLineMapper MAPPER = new InjectorBusinessDataLineMapper(";");

    private CtrBlockFixtures() {
    }

    static BusinessDataLine line(String rawLine, int lineNumber) {
        return MAPPER.mapLine(rawLine, lineNumber);
    }

    /** Two complete contracts, indented exactly as in {@code contracts_input.txt}. */
    static List<String> twoContracts() {
        return List.of(
                "CTR;123456;CLIENT_001;20240101;ACTIVE",
                "  OM;OM_001;CTR_123456;BASE_OFFER",
                "    ART;ART_001;OM_001;INTERNET_SERVICE;100.00;EUR",
                "  COND;COND_001;CTR_123456;COMMITMENT_12M;2024-01-01;2025-01-01",
                "CTR;789012;CLIENT_002;20240215;ACTIVE",
                "  OM;OM_002;CTR_789012;PRO_OFFER");
    }

    static List<BusinessDataLine> asLines(List<String> rawLines) {
        return java.util.stream.IntStream.range(0, rawLines.size())
                .mapToObj(index -> line(rawLines.get(index), index + 1))
                .toList();
    }
}
