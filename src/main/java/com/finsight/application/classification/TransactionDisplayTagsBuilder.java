package com.finsight.application.classification;

import com.finsight.domain.model.Transaction;
import com.finsight.web.api.dto.TransactionDisplayTag;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds {@link TransactionDisplayTag} list from persisted finance semantics (no frontend heuristics).
 */
public final class TransactionDisplayTagsBuilder {

    private static final List<String> SUBSCRIPTION_HINTS = List.of(
            "netflix", "spotify", "apple.com/bill", "adobe", "icloud", "youtube", "amazon prime",
            "订阅", "会员", "月费", "自动续费");
    private static final List<String> TRANSFER_HINTS = List.of(
            "transfer", "atm", "提现", "转账", "汇款", "内部转账");
    private static final List<String> REFUND_HINTS = List.of(
            "refund", "reversal", "chargeback", "退款", "退回", "冲正");

    private TransactionDisplayTagsBuilder() {
    }

    public static List<TransactionDisplayTag> build(Transaction row) {
        List<TransactionDisplayTag> tags = new ArrayList<>();
        if (row == null) {
            return tags;
        }

        String quality = lower(row.getQualityState());
        String economic = lower(row.getEconomicNature());
        String budget = lower(row.getBudgetBehavior());
        String categoryCode = StringUtils.trimToEmpty(row.getConsumeCode());
        String parentId = StringUtils.trimToEmpty(row.getCategoryParentId());
        String fixedKind = CategoryFinanceSemantics.inferFixedCostKind(parentId, categoryCode);
        boolean classified = !"unclassified".equals(quality)
                && StringUtils.isNotBlank(categoryCode);

        if ("unclassified".equals(quality) || (!classified && StringUtils.isBlank(row.getConsumeName()))) {
            tags.add(FinanceSemanticsCatalog.unclassified());
        }

        switch (economic) {
            case "refund" -> tags.add(FinanceSemanticsCatalog.refund());
            case "transfer" -> tags.add(FinanceSemanticsCatalog.transfer());
            case "investment" -> tags.add(FinanceSemanticsCatalog.investment());
            case "liability" -> tags.add(FinanceSemanticsCatalog.liability());
            case "asset_adjustment" -> tags.add(FinanceSemanticsCatalog.transfer());
            case "other" -> {
                if (classified) {
                    tags.add(FinanceSemanticsCatalog.other());
                }
            }
            default -> {
                if ("FEE".equals(categoryRoot(categoryCode)) || categoryCode.startsWith("FEE-")) {
                    tags.add(FinanceSemanticsCatalog.fee());
                }
                if (categoryCode.startsWith("REIM")) {
                    tags.add(FinanceSemanticsCatalog.reimbursement());
                }
            }
        }

        if ("fixed".equals(budget)) {
            if ("subscription".equals(fixedKind)) {
                tags.add(FinanceSemanticsCatalog.subscription());
            } else {
                tags.add(FinanceSemanticsCatalog.fixedCost());
                if (StringUtils.isNotBlank(fixedKind)) {
                    tags.add(FinanceSemanticsCatalog.fixedCostKind(fixedKind));
                }
            }
        } else if ("essential".equals(budget)) {
            tags.add(FinanceSemanticsCatalog.essential());
        }

        if (!classified || "inferred".equals(quality)) {
            appendTextHints(row, tags);
        }

        return dedupe(tags);
    }

    public static String inclusionSummary(Transaction row) {
        if (row == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (Boolean.TRUE.equals(row.getIncludeInIncomeTrend())) {
            parts.add(FinanceSemanticsCatalog.inclusionTrendLabel("income"));
        }
        if (Boolean.TRUE.equals(row.getIncludeInExpenseTrend())) {
            parts.add(FinanceSemanticsCatalog.inclusionTrendLabel("expense"));
        }
        if (Boolean.TRUE.equals(row.getIncludeInBudget())) {
            parts.add(FinanceSemanticsCatalog.inclusionTrendLabel("budget"));
        }
        if (parts.isEmpty()) {
            return "Excluded From Income, Expense, And Budget Trends";
        }
        return String.join(" · ", parts);
    }

    private static void appendTextHints(Transaction row, List<TransactionDisplayTag> tags) {
        String haystack = haystack(row);
        String kind = resolveTxnKind(row);
        if (!"income".equals(kind) && includesAny(haystack, SUBSCRIPTION_HINTS)) {
            tags.add(FinanceSemanticsCatalog.subscriptionHint());
        }
        if (includesAny(haystack, TRANSFER_HINTS)) {
            tags.add(FinanceSemanticsCatalog.transferCandidate());
        }
        if (includesAny(haystack, REFUND_HINTS)) {
            tags.add(FinanceSemanticsCatalog.refundCandidate());
        }
    }

    private static String haystack(Transaction row) {
        return (StringUtils.defaultString(row.getTransactionDesc()) + " "
                + StringUtils.defaultString(row.getDemoArea()) + " "
                + StringUtils.defaultString(row.getConsumeName())).toLowerCase(Locale.ROOT);
    }

    private static String resolveTxnKind(Transaction row) {
        if (StringUtils.isNotBlank(row.getTxnKind())) {
            return row.getTxnKind().trim().toLowerCase(Locale.ROOT);
        }
        if (row.getIncomeMoney() != null && row.getIncomeMoney() > 0) {
            return "income";
        }
        if (row.getBalanceMoney() != null && row.getBalanceMoney() < 0) {
            return "income";
        }
        return "expense";
    }

    private static String categoryRoot(String code) {
        if (StringUtils.isBlank(code)) {
            return "";
        }
        int dash = code.indexOf('-');
        return dash > 0 ? code.substring(0, dash).toUpperCase(Locale.ROOT) : code.toUpperCase(Locale.ROOT);
    }

    private static boolean includesAny(String text, List<String> hints) {
        for (String hint : hints) {
            if (text.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private static List<TransactionDisplayTag> dedupe(List<TransactionDisplayTag> tags) {
        List<TransactionDisplayTag> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (TransactionDisplayTag tag : tags) {
            if (tag == null || seen.contains(tag.getId())) {
                continue;
            }
            seen.add(tag.getId());
            out.add(tag);
        }
        return out;
    }

    private static String lower(String value) {
        return StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
    }
}
