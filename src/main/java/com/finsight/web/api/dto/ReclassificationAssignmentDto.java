package com.finsight.web.api.dto;

/**
 * User-confirmed category for one transaction (Review auto-classify apply).
 */
public class ReclassificationAssignmentDto {

    private String transactionId;
    private String categoryCode;
    private String categoryName;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
