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
            boolean includeInIncomeTrend,
            boolean includeInExpenseTrend,
            boolean includeInBudget) {
    }

    private CategoryFinanceSemantics() {
    }

    public static SemanticProfile profile(String reportRole, String txnTypes) {
        String role = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(reportRole), "other").toLowerCase(Locale.ROOT);
        String txn = StringUtils.defaultString(txnTypes).toLowerCase(Locale.ROOT);
        String economic = economicNature(role);
        String budget = budgetBehavior(role, txn);
        return new SemanticProfile(
                role,
                economic,
                budget,
                includeIncome(role, txn),
                includeExpense(role, txn),
                includeBudget(role, txn));
    }

    public static Map<String, Object> asMap(SemanticProfile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reportRole", p.reportRole());
        m.put("economicNature", p.economicNature());
        m.put("budgetBehavior", p.budgetBehavior());
        m.put("includeInIncomeTrend", p.includeInIncomeTrend());
        m.put("includeInExpenseTrend", p.includeInExpenseTrend());
        m.put("includeInBudget", p.includeInBudget());
        return m;
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

    private static String budgetBehavior(String role, String txn) {
        if ("budget".equals(role) || role.equals("fixed")) {
            return "fixed";
        }
        if ("cashflow".equals(role)) {
            return "essential";
        }
        if (role.equals("other") || role.isBlank()) {
            return "unclassified";
        }
        if (txn.contains("expense") && ("budget".equals(role) || "cashflow".equals(role))) {
            return "variable";
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
        return txn.contains("expense") && ("budget".equals(role) || "cashflow".equals(role) || role.equals("other"));
    }

    private static boolean includeBudget(String role, String txn) {
        if ("transfer".equals(role) || "refund".equals(role) || "investment".equals(role)
                || "liability".equals(role) || "asset".equals(role)) {
            return false;
        }
        return txn.contains("expense") && ("budget".equals(role) || "cashflow".equals(role));
    }
}
