package com.finsight.application.classification;

import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/** Resolves persisted or inferred category semantic_tag for transaction display. */
public final class TransactionSemanticTagResolver {

    private TransactionSemanticTagResolver() {
    }

    public static String resolve(Transaction row) {
        if (row == null) {
            return "other";
        }
        String stored = StringUtils.trimToEmpty(row.getCategorySemanticTag());
        if (StringUtils.isNotBlank(stored)) {
            String normalized = CategoryFlatFixedTags.normalize(
                    stored,
                    row.getCategoryParentId(),
                    row.getConsumeCode());
            return StringUtils.defaultIfBlank(normalized, stored).toLowerCase(Locale.ROOT);
        }
        return infer(row);
    }

    static String infer(Transaction row) {
        String economic = lower(row.getEconomicNature());
        String budget = lower(row.getBudgetBehavior());
        String role = lower(row.getFinanceReportRole());
        String parentId = StringUtils.trimToEmpty(row.getCategoryParentId());
        String categoryCode = StringUtils.trimToEmpty(row.getConsumeCode());
        String fixedKind = CategoryFinanceSemantics.inferFixedCostKind(parentId, categoryCode);

        return switch (economic) {
            case "refund" -> "refund_reimbursement";
            case "transfer" -> "transfer";
            case "investment" -> "investment";
            case "liability" -> "liability";
            case "asset_adjustment" -> "asset_adjustment";
            case "income" -> "investment".equals(role) ? "investment_income" : "real_income";
            case "expense" -> expenseTag(budget, parentId, categoryCode, fixedKind, row.getConsumeName());
            default -> "other";
        };
    }

    private static String expenseTag(String budget, String parentId, String categoryCode, String fixedKind, String name) {
        if ("fixed".equals(budget)) {
            if ("subscription".equals(fixedKind)) {
                return "subscription_spending";
            }
            String flat = CategoryFlatFixedTags.fromCategoryCode(parentId, categoryCode);
            if (flat != null) {
                return flat;
            }
            return CategoryFlatFixedTags.fromFixedKind(fixedKind);
        }
        if ("essential".equals(budget)) {
            return "essential_spending";
        }
        if (CategoryFinanceSemantics.isSocialCategory(parentId, categoryCode)) {
            return "social_spending";
        }
        return CategorySemanticDefaults.inferSemanticTag(
                categoryCode, parentId, name, "expense", "budget", 2);
    }

    private static String lower(String value) {
        return StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
    }
}
