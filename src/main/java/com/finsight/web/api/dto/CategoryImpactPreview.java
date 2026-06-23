package com.finsight.web.api.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Impact preview before category delete, rename, or merge.
 */
public class CategoryImpactPreview {

    private String categoryId;
    private String categoryCode;
    private String categoryName;
    private String action;
    private String targetCode;
    private String targetName;
    private long transactionCount;
    private double totalAmount;
    private long activeRuleCount;
    private long inactiveRuleCount;
    private long childCategoryCount;
    private List<Map<String, Object>> amountByMonth = new ArrayList<>();
    private List<String> affectedReports = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String summary;

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(String targetCode) {
        this.targetCode = targetCode;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getActiveRuleCount() {
        return activeRuleCount;
    }

    public void setActiveRuleCount(long activeRuleCount) {
        this.activeRuleCount = activeRuleCount;
    }

    public long getInactiveRuleCount() {
        return inactiveRuleCount;
    }

    public void setInactiveRuleCount(long inactiveRuleCount) {
        this.inactiveRuleCount = inactiveRuleCount;
    }

    public long getChildCategoryCount() {
        return childCategoryCount;
    }

    public void setChildCategoryCount(long childCategoryCount) {
        this.childCategoryCount = childCategoryCount;
    }

    public List<Map<String, Object>> getAmountByMonth() {
        return amountByMonth;
    }

    public void setAmountByMonth(List<Map<String, Object>> amountByMonth) {
        this.amountByMonth = amountByMonth == null ? new ArrayList<>() : amountByMonth;
    }

    public List<String> getAffectedReports() {
        return affectedReports;
    }

    public void setAffectedReports(List<String> affectedReports) {
        this.affectedReports = affectedReports == null ? new ArrayList<>() : affectedReports;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings == null ? new ArrayList<>() : warnings;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void addMonth(String yearMonth, long txnCount, double amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("yearMonth", yearMonth);
        row.put("txnCount", txnCount);
        row.put("amount", amount);
        amountByMonth.add(row);
    }
}
