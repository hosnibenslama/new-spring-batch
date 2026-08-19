package com.example.mdv02batch.injector.dto;

/**
 * Entity representing a contract row in the {@code contract} table.
 *
 * <p>Mapped from the CTR header line of a {@link CtrBlock}. Each field
 * corresponds to a positional column in the semicolon-delimited input.</p>
 */
public class ContractEntity {

    private String contractId;
    private String clientId;
    private String startDate;
    private String status;
    private int lineCount;

    public ContractEntity() {
    }

    public ContractEntity(String contractId, String clientId, String startDate,
                          String status, int lineCount) {
        this.contractId = contractId;
        this.clientId = clientId;
        this.startDate = startDate;
        this.status = status;
        this.lineCount = lineCount;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }
}
