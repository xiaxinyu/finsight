package com.finsight.web.api.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Category usage, rule coverage, and report impact for the Categories asset panel (Issue #71).
 */
public class CategoryAssetDto {

    private String categoryId;
    private String categoryCode;
    private String categoryName;
    private Integer level;
    private String parentId;
    private long transactionCount;
    private double totalAmount;
    private String lastTransactionDate;
    private long activeRuleCount;
    private long inactiveRuleCount;
    private long orphanRuleCount;
    private long childCategoryCount;
    private List<Map<String, Object>> amountByMonth = new ArrayList<>();
    private List<String> affectedReports = new ArrayList<>();
    private List<String> qualityFlags = new ArrayList<>();
    private List<CategoryChildCandidateDto> childCandidates = new ArrayList<>();
    private String reportRole;
    private String semanticTag;
    private String txnTypes;
    private String economicNature;
    private String budgetBehavior;
    private String fixedCostKind;
    private boolean includeInIncomeTrend;
    private boolean includeInExpenseTrend;
    private boolean includeInBudget;

    public String getReportRole() {
        return reportRole;
    }

    public void setReportRole(String reportRole) {
        this.reportRole = reportRole;
    }

    public String getSemanticTag() {
        return semanticTag;
    }

    public void setSemanticTag(String semanticTag) {
        this.semanticTag = semanticTag;
    }

    public String getTxnTypes() {
        return txnTypes;
    }

    public void setTxnTypes(String txnTypes) {
        this.txnTypes = txnTypes;
    }

    public String getEconomicNature() {
        return economicNature;
    }

    public void setEconomicNature(String economicNature) {
        this.economicNature = economicNature;
    }

    public String getBudgetBehavior() {
        return budgetBehavior;
    }

    public void setBudgetBehavior(String budgetBehavior) {
        this.budgetBehavior = budgetBehavior;
    }

    public String getFixedCostKind() {
        return fixedCostKind;
    }

    public void setFixedCostKind(String fixedCostKind) {
        this.fixedCostKind = fixedCostKind;
    }

    public boolean isIncludeInIncomeTrend() {
        return includeInIncomeTrend;
    }

    public void setIncludeInIncomeTrend(boolean includeInIncomeTrend) {
        this.includeInIncomeTrend = includeInIncomeTrend;
    }

    public boolean isIncludeInExpenseTrend() {
        return includeInExpenseTrend;
    }

    public void setIncludeInExpenseTrend(boolean includeInExpenseTrend) {
        this.includeInExpenseTrend = includeInExpenseTrend;
    }

    public boolean isIncludeInBudget() {
        return includeInBudget;
    }

    public void setIncludeInBudget(boolean includeInBudget) {
        this.includeInBudget = includeInBudget;
    }

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

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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

    public String getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(String lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
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

    public long getOrphanRuleCount() {
        return orphanRuleCount;
    }

    public void setOrphanRuleCount(long orphanRuleCount) {
        this.orphanRuleCount = orphanRuleCount;
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

    public List<String> getQualityFlags() {
        return qualityFlags;
    }

    public void setQualityFlags(List<String> qualityFlags) {
        this.qualityFlags = qualityFlags == null ? new ArrayList<>() : qualityFlags;
    }

    public List<CategoryChildCandidateDto> getChildCandidates() {
        return childCandidates;
    }

    public void setChildCandidates(List<CategoryChildCandidateDto> childCandidates) {
        this.childCandidates = childCandidates == null ? new ArrayList<>() : childCandidates;
    }

    public void addMonth(String yearMonth, long txnCount, double amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("yearMonth", yearMonth);
        row.put("txnCount", txnCount);
        row.put("amount", amount);
        amountByMonth.add(row);
    }
}
