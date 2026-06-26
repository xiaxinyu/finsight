package com.finsight.application.classification;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Category-level finance semantics for admin UI (aligned with v_transaction_finance_semantics defaults).
 */
public final class CategoryFinanceSemantics {

    public record SemanticProfile(
            String reportRole,
            String economicNature,
            String budgetBehavior,
            String fixedCostKind,
            boolean includeInIncomeTrend,
            boolean includeInExpenseTrend,
            boolean includeInBudget) {
    }

    private CategoryFinanceSemantics() {
    }

    public static SemanticProfile profile(String reportRole, String txnTypes) {
        return profile(reportRole, txnTypes, null, null);
    }

    public static SemanticProfile profile(String reportRole, String txnTypes, String parentId, String categoryCode) {
        String role = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(reportRole), "other").toLowerCase(Locale.ROOT);
        String txn = StringUtils.defaultString(txnTypes).toLowerCase(Locale.ROOT);
        String economic = economicNature(role);
        String budget = budgetBehavior(role, txn, parentId, categoryCode);
        String fixedKind = inferFixedCostKind(parentId, categoryCode);
        return new SemanticProfile(
                role,
                economic,
                budget,
                fixedKind,
                includeIncome(role, txn),
                includeExpense(role, txn),
                includeBudget(role, txn));
    }

    public static Map<String, Object> asMap(SemanticProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reportRole", p.reportRole());
        m.put("economicNature", p.economicNature());
        m.put("budgetBehavior", p.budgetBehavior());
        m.put("fixedCostKind", p.fixedCostKind());
        m.put("includeInIncomeTrend", p.includeInIncomeTrend());
        m.put("includeInExpenseTrend", p.includeInExpenseTrend());
        m.put("includeInBudget", p.includeInBudget());
        return m;
    }

    static boolean isFixedCategory(String parentId, String categoryCode) {
        if ("FIXED".equalsIgnoreCase(StringUtils.trimToEmpty(parentId))) {
            return true;
        }
        String code = StringUtils.trimToEmpty(categoryCode).toUpperCase(Locale.ROOT);
        return code.startsWith("FIXED-") || "FIXED".equals(code);
    }

    static boolean isSocialCategory(String parentId, String categoryCode) {
        String parent = StringUtils.trimToEmpty(parentId).toUpperCase(Locale.ROOT);
        if ("GIFT".equals(parent) || "SOCIAL".equals(parent)) {
            return true;
        }
        String code = StringUtils.trimToEmpty(categoryCode).toUpperCase(Locale.ROOT);
        return code.startsWith("GIFT-") || code.startsWith("SOCIAL-");
    }

    public static String inferFixedCostKind(String parentId, String categoryCode) {
        if (!isFixedCategory(parentId, categoryCode)) {
            return null;
        }
        String code = StringUtils.trimToEmpty(categoryCode).toUpperCase(Locale.ROOT);
        return switch (code) {
            case "FIXED-01" -> "rent";
            case "FIXED-02" -> "utilities";
            case "FIXED-03" -> "telecom";
            case "FIXED-04" -> "insurance";
            case "FIXED-05" -> "subscription";
            case "FIXED-06" -> "education";
            case "FIXED-07" -> "repayment";
            case "FIXED-99" -> "other";
            default -> code.startsWith("FIXED-") ? "other" : null;
        };
    }

    private static String economicNature(String role) {
        return switch (role) {
            case "income" -> "income";
            case "refund" -> "refund";
            case "investment" -> "investment";
            case "liability" -> "liability";
            case "asset" -> "asset_adjustment";
            case "transfer" -> "transfer";
            case "budget", "cashflow" -> "expense";
            default -> "other";
        };
    }

    private static String budgetBehavior(String role, String txn, String parentId, String categoryCode) {
        if ("transfer".equals(role) || "refund".equals(role) || "investment".equals(role)
                || "liability".equals(role) || "asset".equals(role)) {
            return "none";
        }
        if (isFixedCategory(parentId, categoryCode)
                && ("budget".equals(role) || "cashflow".equals(role))) {
            return "fixed";
        }
        if ("cashflow".equals(role)) {
            return "essential";
        }
        if ("budget".equals(role) && txn.contains("expense")) {
            return "variable";
        }
        if ("other".equals(role) || role.isBlank()) {
            return "unclassified";
        }
        return "variable";
    }

    private static boolean includeIncome(String role, String txn) {
        return "income".equals(role) && txn.contains("income");
    }

    private static boolean includeExpense(String role, String txn) {
        if ("transfer".equals(role) || "refund".equals(role) || "investment".equals(role)
                || "liability".equals(role) || "asset".equals(role)) {
            return false;
        }
        return txn.contains("expense") && ("budget".equals(role) || "cashflow".equals(role) || "other".equals(role));
    }

    private static boolean includeBudget(String role, String txn) {
        if ("transfer".equals(role) || "refund".equals(role) || "investment".equals(role)
                || "liability".equals(role) || "asset".equals(role)) {
            return false;
        }
        return txn.contains("expense") && ("budget".equals(role) || "cashflow".equals(role));
    }
}
