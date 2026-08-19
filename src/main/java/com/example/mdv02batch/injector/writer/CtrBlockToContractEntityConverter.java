package com.example.mdv02batch.injector.writer;

import java.util.List;

import com.example.mdv02batch.injector.dto.BusinessDataLine;
import com.example.mdv02batch.injector.dto.ContractEntity;
import com.example.mdv02batch.injector.dto.CtrBlock;

/**
 * Converts a {@link CtrBlock} into a {@link ContractEntity} for database
 * persistence.
 *
 * <p>Extracts the positional fields from the CTR header line:
 * {@code CTR;contractId;clientId;startDate;status}. Orphan blocks (no header)
 * are converted into entities with {@code null} identifiers so the writer can
 * skip them.</p>
 */
public final class CtrBlockToContractEntityConverter {

    private CtrBlockToContractEntityConverter() {
    }

    /**
     * Converts the block to an entity. Returns {@code null} if the block is
     * an orphan (no CTR header).
     */
    public static ContractEntity convert(CtrBlock block) {
        if (block.orphan()) {
            return null;
        }

        BusinessDataLine header = block.header();
        List<String> fields = header.fields();

        String contractId = fields.size() > 1 ? fields.get(1) : null;
        String clientId   = fields.size() > 2 ? fields.get(2) : null;
        String startDate  = fields.size() > 3 ? fields.get(3) : null;
        String status     = fields.size() > 4 ? fields.get(4) : null;

        return new ContractEntity(contractId, clientId, startDate, status, block.lineCount());
    }
}
