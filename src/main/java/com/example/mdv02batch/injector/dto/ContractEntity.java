package com.example.mdv02batch.injector.dto;

/**
 * Entity representing a contract row in the {@code contract} table.
 *
 * <p>Mapped from the CTR header line of a {@link CtrBlock}. Each field
 * corresponds to a positional column in the semicolon-delimited input.</p>
 */
public record ContractEntity(
        String contractId,
        String clientId,
        String startDate,
        String status,
        int lineCount) {
}
