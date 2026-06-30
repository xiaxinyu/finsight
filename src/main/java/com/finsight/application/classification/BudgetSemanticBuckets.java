package com.finsight.application.classification;

import org.apache.commons.lang3.StringUtils;

/**
 * Maps planning budget bucket keys to {@code v_transaction_finance_semantics} predicates.
 */
public final class BudgetSemanticBuckets {

    private BudgetSemanticBuckets() {
    }

    public static String displayLabel(String bucketKey) {
        if (StringUtils.isBlank(bucketKey) || "all".equalsIgnoreCase(bucketKey)) {
            return "Expense / All budgetable";
        }
        return switch (bucketKey.trim().toLowerCase()) {
            case "fixed" -> "Fixed / All fixed";
            case "shopping" -> "Expense / Shopping & groceries";
            case "life" -> "Expense / Variable daily";
            case "investment" -> "Finance / Investment";
            default -> "Expense / " + bucketKey.trim();
        };
    }

    /**
     * SQL fragment appended after base expense-trend filter (starts with {@code and}).
     */
    public static String sqlPredicate(String bucketKey) {
        String bucket = StringUtils.isBlank(bucketKey) ? "all" : bucketKey.trim().toLowerCase();
        return switch (bucket) {
            case "fixed" -> " and s.budget_behavior = 'fixed'";
            case "shopping" -> " and s.semantic_tag in ('shopping_spending', 'groceries_spending')";
            case "life" -> " and s.include_in_expense_trend = 1 and s.budget_behavior = 'variable'";
            case "investment" -> " and s.semantic_tag = 'investment'";
            case "all" -> "";
            default -> " and (s.category_code = ? or s.category_l1_code = ?)";
        };
    }

    public static boolean usesCategoryBind(String bucketKey) {
        String bucket = StringUtils.isBlank(bucketKey) ? "all" : bucketKey.trim().toLowerCase();
        return !"fixed".equals(bucket) && !"shopping".equals(bucket) && !"life".equals(bucket)
                && !"investment".equals(bucket) && !"all".equals(bucket);
    }
}
