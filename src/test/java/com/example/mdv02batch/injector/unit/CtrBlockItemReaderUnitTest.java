package com.example.mdv02batch.injector.unit;

import java.util.ArrayList;
import java.util.List;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.dto.CtrBlock;
import com.example.mdv02batch.injector.reader.CtrBlockItemReader;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.support.ListItemReader;

import static org.assertj.core.api.Assertions.assertThat;

class CtrBlockItemReaderUnitTest {

    private static CtrBlockItemReader readerOf(List<String> rawLines) {
        CtrBlockItemReader reader = new CtrBlockItemReader(
                new ListItemReader<>(new ArrayList<>(CtrBlockFixtures.asLines(rawLines))), "CTR");
        reader.open(new ExecutionContext());
        return reader;
    }

    private static List<CtrBlock> readAll(CtrBlockItemReader reader) throws Exception {
        List<CtrBlock> blocks = new ArrayList<>();
        CtrBlock block;
        while ((block = reader.read()) != null) {
            blocks.add(block);
        }
        return blocks;
    }

    @Test
    void shouldGroupEachCtrHeaderWithItsSubordinateLines() throws Exception {
        List<CtrBlock> blocks = readAll(readerOf(CtrBlockFixtures.twoContracts()));

        assertThat(blocks).hasSize(2);

        CtrBlock first = blocks.get(0);
        assertThat(first.contractId()).isEqualTo("123456");
        assertThat(first.startLineNumber()).isEqualTo(1);
        assertThat(first.lineCount()).isEqualTo(4);
        assertThat(first.children()).extracting(BusinessDataLine::recordType)
                .containsExactly("OM", "ART", "COND");

        CtrBlock second = blocks.get(1);
        assertThat(second.contractId()).isEqualTo("789012");
        assertThat(second.startLineNumber()).isEqualTo(5);
        assertThat(second.lineCount()).isEqualTo(2);
    }

    @Test
    void shouldNotLoseAnyLineWhileGrouping() throws Exception {
        List<String> rawLines = CtrBlockFixtures.twoContracts();

        List<String> regrouped = readAll(readerOf(rawLines)).stream()
                .flatMap(block -> block.lines().stream())
                .map(BusinessDataLine::rawLine)
                .toList();

        assertThat(regrouped).containsExactlyElementsOf(rawLines);
    }

    @Test
    void shouldReadASingleLineContractAsABlockWithoutChild() throws Exception {
        List<CtrBlock> blocks = readAll(readerOf(List.of(
                "CTR;123456;CLIENT_001;20240101;ACTIVE",
                "CTR;789012;CLIENT_002;20240215;ACTIVE")));

        assertThat(blocks).hasSize(2);
        assertThat(blocks).allSatisfy(block -> assertThat(block.children()).isEmpty());
    }

    @Test
    void shouldExposeLeadingLinesWithoutHeaderAsAnOrphanBlock() throws Exception {
        List<CtrBlock> blocks = readAll(readerOf(List.of(
                "  OM;OM_999;CTR_UNKNOWN;BASE_OFFER",
                "CTR;123456;CLIENT_001;20240101;ACTIVE",
                "  OM;OM_001;CTR_123456;BASE_OFFER")));

        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).orphan()).isTrue();
        assertThat(blocks.get(0).contractId()).isNull();
        assertThat(blocks.get(0).lineCount()).isEqualTo(1);
        assertThat(blocks.get(1).orphan()).isFalse();
    }

    @Test
    void shouldReturnNullOnAnEmptySource() throws Exception {
        assertThat(readerOf(List.of()).read()).isNull();
    }

    @Test
    void shouldFindLinesByRecordType() throws Exception {
        CtrBlock block = readAll(readerOf(CtrBlockFixtures.twoContracts())).get(0);

        assertThat(block.linesOfType("ART")).hasSize(1);
        assertThat(block.firstOfType("COND")).isPresent();
        assertThat(block.firstOfType("OFF")).isEmpty();
    }
}
