package com.finsight.application.classification;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Default {@code semantic_tag} + {@code report_role} for {@code cls_category} rows.
 * User-set values are preserved; blanks are filled from catalog + parent/code heuristics.
 */
public final class CategorySemanticDefaults {

    public record CategoryInput(
            String code,
            String name,
            int level,
            String parentId,
            String txnTypes,
            String reportRole,
            String semanticTag) {
    }

    public record ResolvedDefaults(String reportRole, String semanticTag) {
    }

    private CategorySemanticDefaults() {
    }

    /** Fill only missing report_role / semantic_tag (does not overwrite user values). */
    public static ResolvedDefaults fillMissing(CategoryInput row) {
        if (row == null || StringUtils.isBlank(row.code())) {
            return new ResolvedDefaults("other", "other");
        }
        String reportRole = resolveReportRole(row);
        String semanticTag = StringUtils.isNotBlank(row.semanticTag())
                ? row.semanticTag().trim().toLowerCase(Locale.ROOT)
                : inferSemanticTag(row.code(), row.parentId(), row.name(), row.txnTypes(), reportRole, row.level());
        return new ResolvedDefaults(reportRole, semanticTag);
    }

    public static String inferSemanticTag(
            String code,
            String parentId,
            String name,
            String txnTypes,
            String reportRole,
            int level) {
        Optional<ClassificationL2TargetCatalog> catalog = ClassificationL2TargetCatalog.byCode(code);
        if (catalog.isPresent()) {
            return inferFromCatalog(catalog.get());
        }
        if (level <= 1) {
            return inferL1SemanticTag(code, txnTypes);
        }
        return inferFromHeuristics(code, parentId, name, txnTypes, reportRole);
    }

    static String inferFromCatalog(ClassificationL2TargetCatalog target) {
        return switch (target.code()) {
            case "FIXED-01" -> "fixed_housing";
            case "FIXED-02" -> "fixed_utilities";
            case "FIXED-03" -> "fixed_telecom";
            case "FIXED-04" -> "fixed_insurance";
            case "FIXED-05" -> "subscription_spending";
            case "FIXED-06" -> "fixed_tuition";
            case "FIXED-07" -> "fixed_repayment";
            case "FIXED-99" -> "fixed_misc";
            case "TRANS-06", "DEBT-05" -> "essential_spending";
            case "EDU-01" -> "essential_spending";
            case "GIFT-02", "ASSET-02", "INVEST-05" -> "transfer";
            case "INC-04", "INCOME-03", "INV-04", "INV-06", "WEALTH-02" -> "investment_income";
            case "INC-08", "DEBT-02" -> "liability";
            case "INC-10" -> "refund_reimbursement";
            case "REIM-01", "REIM-02", "REIM-03", "REIM-04", "REIM-05" -> "refund_reimbursement";
            case "OTHER-01", "OTHER-02", "OTHER-03" -> "other_expense";
            case "DAILY-01", "DAILY-02" -> "dining_spending";
            case "DAILY-03", "DAILY-04" -> "shopping_spending";
            case "DAILY-05" -> "medical_spending";
            case "SHOP-01", "SHOP-02", "SHOP-03", "SHOP-04", "SHOP-05", "SHOP-06" -> "shopping_spending";
            case "TRAVEL-01", "TRANS-02", "TRANS-03", "TRANS-04", "TRANS-05", "TRANS-07" -> "transport_spending";
            case "ENT-01", "ENT-02", "ENT-03", "ENT-04", "ENT-05", "ENT-06" -> "entertainment_spending";
            case "EDU-02" -> "education_spending";
            default -> inferFromHeuristics(
                    target.code(),
                    target.parentL1Code(),
                    target.displayName(),
                    target.txnTypes(),
                    target.reportRole());
        };
    }

    static String inferL1SemanticTag(String code, String txnTypes) {
        if (StringUtils.isBlank(code)) {
            return "other";
        }
        String c = code.trim().toUpperCase(Locale.ROOT);
        return switch (c) {
            case "INC", "INCOME" -> "real_income";
            case "FIXED" -> "fixed_housing";
            case "LIVING" -> "daily_spending";
            case "SHOPPING" -> "shopping_spending";
            case "TRANSPORT", "TRAVEL" -> "transport_spending";
            case "ENT" -> "entertainment_spending";
            case "EDU" -> "education_spending";
            case "GIFT", "SOCIAL" -> "social_spending";
            case "REIM", "REIMB" -> "refund_reimbursement";
            case "ASSET" -> "asset_adjustment";
            case "LIABILITY" -> "liability";
            case "INVEST", "WEALTH", "FP" -> "investment";
            case "FEE", "FE" -> "essential_spending";
            case "OTHER" -> "other_expense";
            default -> inferL1FromTxnTypes(txnTypes);
        };
    }

    private static String inferL1FromTxnTypes(String txnTypes) {
        String txn = StringUtils.defaultString(txnTypes).toLowerCase(Locale.ROOT);
        if (txn.contains("income") && !txn.contains("expense")) {
            return "real_income";
        }
        if (txn.contains("refund")) {
            return "refund_reimbursement";
        }
        if (txn.contains("transfer")) {
            return "transfer";
        }
        if (txn.contains("invest")) {
            return "investment";
        }
        if (txn.contains("liability")) {
            return "liability";
        }
        if (txn.contains("expense")) {
            return "daily_spending";
        }
        return "other";
    }

    static String inferFromHeuristics(String code, String parentId, String name, String txnTypes, String reportRole) {
        String c = StringUtils.trimToEmpty(code).toUpperCase(Locale.ROOT);
        String parent = StringUtils.trimToEmpty(parentId).toUpperCase(Locale.ROOT);
        String n = StringUtils.defaultString(name);
        String txn = StringUtils.defaultString(txnTypes).toLowerCase(Locale.ROOT);
        String role = StringUtils.defaultIfBlank(StringUtils.trimToEmpty(reportRole), "other").toLowerCase(Locale.ROOT);

        if (parent.equals("FIXED") || c.startsWith("FIXED-")) {
            if (c.equals("FIXED-05") || n.contains("订阅") || n.contains("会员")) {
                return "subscription_spending";
            }
            String flat = CategoryFlatFixedTags.fromCategoryCode(parent, c);
            return flat != null ? flat : "fixed_misc";
        }
        if (parent.equals("FEE") || parent.equals("FE") || c.startsWith("FEE-")) {
            return "essential_spending";
        }
        if (parent.equals("GIFT") || parent.equals("SOCIAL") || c.startsWith("GIFT-")) {
            if (n.contains("转账") || txn.contains("transfer")) {
                return "transfer";
            }
            return "social_spending";
        }
        if (parent.equals("REIM") || parent.equals("REIMB") || c.startsWith("REIM-")) {
            return "refund_reimbursement";
        }
        if (parent.equals("INC") || parent.equals("INCOME") || c.startsWith("INC-") || c.startsWith("INCOME-")) {
            if (c.equals("INC-04") || c.equals("INCOME-03") || txn.contains("invest")) {
                return "investment_income";
            }
            if (c.equals("INC-08") || txn.contains("liability")) {
                return "liability";
            }
            if (c.equals("INC-10") || n.contains("报销") || n.contains("退款")) {
                return "refund_reimbursement";
            }
            return "real_income";
        }
        if (parent.equals("ASSET") || c.startsWith("ASSET-")) {
            if (n.contains("转出") || n.contains("转入") || n.contains("提现") || c.equals("ASSET-02")) {
                return "transfer";
            }
            return "asset_adjustment";
        }
        if (parent.equals("LIABILITY") || c.startsWith("DEBT-")) {
            if (n.contains("利息") || c.equals("DEBT-05")) {
                return "essential_spending";
            }
            if (c.equals("FIXED-07") || n.contains("还款")) {
                return "fixed_repayment";
            }
            return "liability";
        }
        if (parent.equals("INVEST") || parent.equals("WEALTH") || parent.equals("FP")
                || c.startsWith("INVEST-") || c.startsWith("WEALTH-")) {
            if (txn.contains("income") && (n.contains("卖") || n.contains("赎") || c.contains("04") || c.contains("06"))) {
                return "investment_income";
            }
            if (c.equals("INVEST-05")) {
                return "transfer";
            }
            return "investment";
        }
        if (parent.equals("OTHER") || c.startsWith("OTHER-")) {
            return "other_expense";
        }
        if (c.equals("EDU-01") || n.contains("学费")) {
            return "essential_spending";
        }
        if (c.equals("TRANS-06") || (n.contains("保险") && parent.equals("TRANSPORT"))) {
            return "essential_spending";
        }

        Optional<String> domain = CategorySpendingDomain.inferDomainTag(c, parent, n);
        if (domain.isPresent()) {
            return domain.get();
        }

        if (parent.equals("TRANSPORT") || parent.equals("TRAVEL")) {
            return "transport_spending";
        }
        if (parent.equals("SHOPPING")) {
            return "shopping_spending";
        }
        if (parent.equals("ENT")) {
            return "entertainment_spending";
        }
        if (parent.equals("EDU")) {
            return "education_spending";
        }
        if (parent.equals("LIVING")) {
            return "daily_spending";
        }
        return inferFromReportRole(role, parent, c, txn, n);
    }

    static String inferFromReportRole(String role, String parent, String code, String txn, String name) {
        return switch (role) {
            case "income" -> (code.startsWith("INC-04") || txn.contains("invest"))
                    ? "investment_income" : "real_income";
            case "refund" -> "refund_reimbursement";
            case "transfer" -> "transfer";
            case "investment" -> (parent.equals("INC") || parent.equals("INCOME") || code.startsWith("INC-04"))
                    ? "investment_income" : "investment";
            case "liability" -> {
                if (parent.equals("FIXED") || code.startsWith("FIXED-")) {
                    String flat = CategoryFlatFixedTags.fromCategoryCode(parent, code);
                    yield flat != null ? flat : "fixed_repayment";
                }
                yield "liability";
            }
            case "asset" -> "asset_adjustment";
            case "cashflow" -> {
                if (parent.equals("FIXED") || code.startsWith("FIXED-")) {
                    String flat = CategoryFlatFixedTags.fromCategoryCode(parent, code);
                    yield flat != null ? flat : "fixed_misc";
                }
                yield "essential_spending";
            }
            case "budget" -> {
                if (code.equals("FIXED-05")) {
                    yield "subscription_spending";
                }
                if (parent.equals("FIXED") || code.startsWith("FIXED-")) {
                    String flat = CategoryFlatFixedTags.fromCategoryCode(parent, code);
                    yield flat != null ? flat : "fixed_misc";
                }
                if (parent.equals("GIFT") || parent.equals("SOCIAL") || code.startsWith("GIFT-")) {
                    yield "social_spending";
                }
                if (parent.equals("OTHER") || code.startsWith("OTHER-")) {
                    yield "other_expense";
                }
                yield CategorySpendingDomain.inferDomainTag(code, parent, name).orElse("daily_spending");
            }
            default -> "other";
        };
    }

    private static String resolveReportRole(CategoryInput row) {
        String explicit = CategoryReportRoles.normalize(row.reportRole());
        if (explicit != null) {
            return explicit;
        }
        return CategoryReportRoleInference.inferReportRole(
                new CategoryReportRoleInference.DbCategoryRow(
                        row.code(),
                        row.name(),
                        row.level(),
                        row.parentId(),
                        row.txnTypes()))
                .orElse("other");
    }
}
