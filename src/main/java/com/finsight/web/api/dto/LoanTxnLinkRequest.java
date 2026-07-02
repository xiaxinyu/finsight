package com.finsight.web.api.dto;

public class LoanTxnLinkRequest {
    private String transactionId;
    private String linkType;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getLinkType() { return linkType; }
    public void setLinkType(String linkType) { this.linkType = linkType; }
}
