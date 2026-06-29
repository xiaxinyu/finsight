package com.finsight.application.classification;

import com.finsight.web.api.dto.TransactionDisplayTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for finance semantic labels (API catalog + transaction display tags).
 * Taxonomy aligns with personal-finance report buckets: Income Statement, Fixed Commitments, Capital Flows.
 */
public final class FinanceSemanticsCatalog {

    private FinanceSemanticsCatalog() {
    }

    public static TransactionDisplayTag tag(String id, String label, String color, String hint) {
        return new TransactionDisplayTag(id, label, color, hint);
    }

    public static TransactionDisplayTag unclassified() {
        return tag("unclassified", "Unclassified", "gold", "No Category Assigned");
    }

    public static TransactionDisplayTag refund() {
        return tag("refund", "Refund", "blue", "Excluded From Income Trend");
    }

    public static TransactionDisplayTag reimbursement() {
        return tag("reimbursement", "Reimbursement", "cyan", "Not Counted As Real Income");
    }

    public static TransactionDisplayTag transfer() {
        return tag("transfer", "Transfer", "geekblue", "Excluded From Income And Expense Trends");
    }

    public static TransactionDisplayTag investment() {
        return tag("investment", "Investment", "purple", "Investment Cash Flow, Not Consumption");
    }

    public static TransactionDisplayTag investmentIncome() {
        return tag("investment_income", "Portfolio", "purple", "Dividends, Interest, And Investment Gains");
    }

    public static TransactionDisplayTag liability() {
        return tag("liability", "Debt", "volcano", "Borrowing Or Repayment, Not Consumption");
    }

    public static TransactionDisplayTag fee() {
        return tag("fee", "Fee", "magenta", "Bank Or Finance Fee");
    }

    public static TransactionDisplayTag other() {
        return tag("other", "Other", "default", "Review For Data Quality");
    }

    public static TransactionDisplayTag fixedCost() {
        return tag("fixed_cost", "Fixed", "purple", "Rent, Utilities, Insurance, Or Similar");
    }

    public static TransactionDisplayTag subscription() {
        return tag("subscription", "Subscription", "geekblue", "Recurring Membership Or Service Fee");
    }

    public static TransactionDisplayTag fixedCostKind(String kind) {
        return tag("fixed_cost_" + kind, fixedCostKindLabel(kind), "purple", fixedCostKindHint(kind));
    }

    public static TransactionDisplayTag essential() {
        return tag("essential", "Essential", "cyan", "Required Cash Flow Expense");
    }

    public static TransactionDisplayTag social() {
        return tag("social", "Social", "magenta", "Gifts, Red Envelopes, Donations, And Social Giving");
    }

    public static TransactionDisplayTag subscriptionHint() {
        return subscription();
    }

    public static TransactionDisplayTag transferCandidate() {
        return tag("transfer_candidate", "Transfer Candidate", "cyan", "May Be An Internal Transfer");
    }

    public static TransactionDisplayTag refundCandidate() {
        return tag("refund_candidate", "Refund Candidate", "blue", "Possible Refund Or Reversal");
    }

    /** Primary reporting classification chip (Discretionary, Fixed, Earned, …). */
    public static TransactionDisplayTag semanticClassification(String tagId) {
        if (tagId == null || tagId.isBlank()) {
            return null;
        }
        String id = tagId.trim();
        return tag("semantic_" + id, semanticTagLabel(id), semanticTagColor(id), semanticTagDescription(id));
    }

    public static TransactionDisplayTag categoryGroup(String l1Name) {
        if (l1Name == null || l1Name.isBlank()) {
            return null;
        }
        return tag("category_l1", l1Name.trim(), "default", "Top-Level Category Group");
    }

    private static String semanticTagColor(String tagId) {
        return switch (tagId) {
            case "real_income" -> "green";
            case "investment_income" -> "purple";
            case "other_income" -> "lime";
            case "refund_reimbursement" -> "blue";
            case "daily_spending" -> "orange";
            case "dining_spending" -> "orange";
            case "shopping_spending" -> "gold";
            case "transport_spending" -> "blue";
            case "entertainment_spending" -> "volcano";
            case "education_spending" -> "cyan";
            case "social_spending" -> "magenta";
            case "other_expense" -> "default";
            case "fixed_spending" -> "purple";
            case "subscription_spending" -> "geekblue";
            case "essential_spending" -> "cyan";
            case "transfer" -> "geekblue";
            case "investment" -> "purple";
            case "liability" -> "volcano";
            case "asset_adjustment" -> "gold";
            default -> "default";
        };
    }

    public static String fixedCostKindLabel(String kind) {
        if (kind == null) {
            return "";
        }
        return switch (kind) {
            case "rent" -> "Housing";
            case "utilities" -> "Utilities";
            case "telecom" -> "Telecom";
            case "insurance" -> "Insurance";
            case "subscription" -> "Subscription";
            case "education" -> "Education";
            case "repayment" -> "Repayment";
            case "other" -> "Other";
            default -> kind;
        };
    }

    private static String fixedCostKindHint(String kind) {
        return "Fixed · " + fixedCostKindLabel(kind);
    }

    public static String semanticTagLabel(String tagId) {
        return switch (tagId) {
            case "real_income" -> "Earned";
            case "investment_income" -> "Portfolio";
            case "other_income" -> "MiscIncome";
            case "refund_reimbursement" -> "Refund";
            case "daily_spending" -> "General";
            case "dining_spending" -> "Dining";
            case "shopping_spending" -> "Shopping";
            case "transport_spending" -> "Transport";
            case "entertainment_spending" -> "Entertainment";
            case "education_spending" -> "Education";
            case "social_spending" -> "Social";
            case "other_expense" -> "MiscExpense";
            case "fixed_spending" -> "Fixed";
            case "subscription_spending" -> "Subscription";
            case "essential_spending" -> "Essential";
            case "transfer" -> "Transfer";
            case "investment" -> "Investment";
            case "liability" -> "Debt";
            case "asset_adjustment" -> "Rebalance";
            case "other" -> "Unset";
            default -> titleCaseWords(tagId.replace('_', ' '));
        };
    }

    public static String budgetBehaviorLabel(String behavior) {
        return switch (behavior) {
            case "fixed" -> "Fixed";
            case "variable" -> "Discretionary";
            case "essential" -> "Essential";
            case "unclassified" -> "Unclassified";
            case "none" -> "N/A";
            default -> behavior;
        };
    }

    public static String inclusionTrendLabel(String key) {
        return switch (key) {
            case "income" -> "IncomeTrend";
            case "expense" -> "ExpenseTrend";
            case "budget" -> "Budget";
            case "fixed_cost" -> "FixedCost";
            case "cashflow" -> "CashFlow";
            default -> key;
        };
    }

    public static Map<String, Object> catalogPayload() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fieldLabel", "Reporting Classification");
        out.put("fieldHint", "Pick expense type (Dining/Shopping/Transport) and report behavior (Social/Fixed/Essential).");

        List<Map<String, Object>> groups = new ArrayList<>();
        groups.add(tagGroup("Income", "income",
                List.of("real_income", "investment_income", "refund_reimbursement", "other_income")));
        groups.add(tagGroup("Expense", "expense",
                List.of(
                        "dining_spending", "shopping_spending", "transport_spending",
                        "entertainment_spending", "education_spending",
                        "social_spending", "subscription_spending", "essential_spending",
                        "daily_spending", "other_expense")));
        groups.add(tagGroup("Fixed", "expense",
                List.of("fixed_spending")));
        groups.add(tagGroup("Capital", "capital",
                List.of("transfer", "investment", "liability", "asset_adjustment")));
        out.put("semanticTagGroups", groups);

        Map<String, Object> semanticTags = new LinkedHashMap<>();
        for (String id : List.of(
                "real_income", "investment_income", "other_income", "refund_reimbursement",
                "dining_spending", "shopping_spending", "transport_spending", "entertainment_spending", "education_spending",
                "daily_spending", "social_spending", "other_expense", "fixed_spending", "subscription_spending", "essential_spending",
                "transfer", "investment", "liability", "asset_adjustment", "other")) {
            semanticTags.put(id, Map.of(
                    "id", id,
                    "label", semanticTagLabel(id),
                    "description", semanticTagDescription(id),
                    "reportBucket", reportBucket(id)));
        }
        out.put("semanticTags", semanticTags);

        Map<String, Object> fixedCostKinds = new LinkedHashMap<>();
        for (String kind : List.of(
                "rent", "utilities", "telecom", "insurance", "subscription", "education", "repayment", "other")) {
            fixedCostKinds.put(kind, Map.of("id", kind, "label", fixedCostKindLabel(kind)));
        }
        out.put("fixedCostKinds", fixedCostKinds);
        out.put("fixedCostKindSectionLabel", "FixedType");

        out.put("budgetBehaviors", Map.of(
                "fixed", budgetBehaviorLabel("fixed"),
                "variable", budgetBehaviorLabel("variable"),
                "essential", budgetBehaviorLabel("essential")));

        out.put("reportSurfaces", List.of(
                surface("income", "IncomeTrend"),
                surface("expense", "ExpenseTrend"),
                surface("budget", "Budget"),
                surface("fixed_cost", "FixedCost"),
                surface("cashflow", "CashFlow")));

        out.put("previewSectionLabel", "ReportPreview");
        return out;
    }

    private static String semanticTagDescription(String tagId) {
        return switch (tagId) {
            case "real_income" -> "Salary, Bonus, And Primary Earned Income";
            case "investment_income" -> "Dividends, Interest, And Portfolio Income";
            case "other_income" -> "Miscellaneous Inflow Not In Core Income";
            case "refund_reimbursement" -> "Refund Or Expense Reimbursement Inflow";
            case "daily_spending" -> "General Discretionary Expense";
            case "dining_spending" -> "Restaurants, Takeout, Coffee, And Meals";
            case "shopping_spending" -> "Groceries, Retail, E-Commerce, And Durable Goods";
            case "transport_spending" -> "Transit, Ride-Hail, Fuel, Parking, And Vehicle Costs";
            case "entertainment_spending" -> "Travel, Leisure, Sports, And Entertainment";
            case "education_spending" -> "Courses, Books, And Training (Non-Fixed Tuition)";
            case "social_spending" -> "Gifts, Red Envelopes, Donations, And Family Support";
            case "other_expense" -> "Miscellaneous Expense Not In Core Buckets";
            case "fixed_spending" -> "Recurring Fixed Obligations";
            case "subscription_spending" -> "Recurring Subscriptions And Memberships";
            case "essential_spending" -> "Required Non-Discretionary Cash Outflow";
            case "transfer" -> "Internal Transfer Or Balance Adjustment";
            case "investment" -> "Investment Buy, Sell, Or Transfer";
            case "liability" -> "Borrowing, Repayment, Or Debt Service";
            case "asset_adjustment" -> "Asset Purchase, Sale, Or Rebalance";
            case "other" -> "Unset Classification — Fix Before Reporting";
            default -> "";
        };
    }

    private static String reportBucket(String tagId) {
        return switch (tagId) {
            case "real_income", "investment_income", "other_income", "refund_reimbursement" -> "income_statement";
            case "daily_spending", "dining_spending", "shopping_spending", "transport_spending",
                    "entertainment_spending", "education_spending",
                    "social_spending", "other_expense", "subscription_spending", "essential_spending" -> "expense";
            case "fixed_spending" -> "fixed_commitment";
            case "transfer", "investment", "liability", "asset_adjustment" -> "capital_flow";
            default -> "unset";
        };
    }

    private static Map<String, Object> tagGroup(String title, String appliesTo, List<String> tags) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("title", title);
        g.put("appliesTo", appliesTo);
        g.put("tags", tags);
        return g;
    }

    private static Map<String, Object> surface(String id, String label) {
        return Map.of("id", id, "label", label);
    }

    private static String titleCaseWords(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] parts = raw.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
