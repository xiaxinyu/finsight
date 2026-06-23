package com.finsight.web.api.dto;

/**
 * Lightweight per-category stats for tree badges on the Categories page.
 */
public class CategoryAssetSummaryRow {

    private String categoryCode;
    private long transactionCount;
    private long activeRuleCount;

    public CategoryAssetSummaryRow() {
    }

    public CategoryAssetSummaryRow(String categoryCode, long transactionCount, long activeRuleCount) {
        this.categoryCode = categoryCode;
        this.transactionCount = transactionCount;
        this.activeRuleCount = activeRuleCount;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public long getActiveRuleCount() {
        return activeRuleCount;
    }

    public void setActiveRuleCount(long activeRuleCount) {
        this.activeRuleCount = activeRuleCount;
    }
}
