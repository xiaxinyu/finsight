package com.finsight.application.classification;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Infers {@code report_role} for live {@code cls_category} rows when catalog has no exact match.
 */
public final class CategoryReportRoleInference {

    public record DbCategoryRow(
            String code,
            String name,
            int level,
            String parentId,
            String txnTypes) {
    }

    private CategoryReportRoleInference() {
    }

    public static Optional<String> inferReportRole(DbCategoryRow row) {
        if (row == null || StringUtils.isBlank(row.code())) {
            return Optional.empty();
        }
        Optional<ClassificationL2TargetCatalog> catalog = ClassificationL2TargetCatalog.byCode(row.code());
        if (catalog.isPresent()) {
            return Optional.of(catalog.get().reportRole());
        }
        String code = row.code().trim();
        String parent = StringUtils.trimToEmpty(row.parentId());
        String name = StringUtils.defaultString(row.name());
        String txn = StringUtils.defaultString(row.txnTypes()).toLowerCase(Locale.ROOT);

        if (row.level() <= 1) {
            return inferL1ReportRole(code, txn);
        }

        if (parent.equals("INC") || parent.equals("INCOME")) {
            if (code.startsWith("INC-04") || txn.contains("invest")) {
                return Optional.of("investment");
            }
            if (code.startsWith("INC-08") || txn.contains("liability")) {
                return Optional.of("liability");
            }
            if (name.contains("报销") || name.contains("退款") || txn.contains("refund")) {
                return Optional.of("refund");
            }
            return Optional.of("income");
        }
        if (parent.equals("REIM") || parent.equals("REIMB")) {
            return Optional.of("refund");
        }
        if (parent.equals("ASSET")) {
            if (name.contains("转出") || name.contains("取出") || name.contains("提现") || txn.contains("transfer")) {
                return Optional.of("transfer");
            }
            return Optional.of("asset");
        }
        if (parent.equals("LIABILITY") || code.startsWith("DEBT-")) {
            if (name.contains("利息") || txn.contains("expense")) {
                return Optional.of("cashflow");
            }
            return Optional.of("liability");
        }
        if (parent.equals("INVEST") || parent.equals("WEALTH") || parent.equals("FP")) {
            return Optional.of("investment");
        }
        if (parent.equals("FE") || parent.equals("FEE")) {
            return Optional.of("cashflow");
        }
        if (parent.equals("GIFT") || parent.equals("SOCIAL")) {
            if (name.contains("转账") || txn.contains("transfer")) {
                return Optional.of("transfer");
            }
            if (name.contains("接收") || name.contains("收到")) {
                return Optional.of("income");
            }
            return Optional.of("budget");
        }
        if (parent.equals("FIXED")) {
            if (name.contains("保险") || name.contains("订阅") || code.equals("FIXED-07")) {
                return Optional.of("cashflow");
            }
            if (name.contains("还款") || name.contains("车贷") || txn.contains("liability")) {
                return Optional.of("liability");
            }
            return Optional.of("budget");
        }
        if (parent.equals("TRANSPORT")) {
            if (name.contains("保险")) {
                return Optional.of("cashflow");
            }
            return Optional.of("budget");
        }
        if (parent.equals("OTHER")) {
            return Optional.of("budget");
        }
        if (parent.equals("LIVING") || parent.equals("SHOPPING") || parent.equals("EDU")
                || parent.equals("ENT") || code.startsWith("DAILY-") || code.startsWith("LIVING-")
                || code.startsWith("SHOP-") || code.startsWith("SHOPPING-")
                || code.startsWith("TRANS-") || code.startsWith("TRANSPORT-")) {
            return Optional.of("budget");
        }
        return Optional.empty();
    }

    private static Optional<String> inferL1ReportRole(String code, String txn) {
        return switch (code) {
            case "INC", "INCOME" -> Optional.of("income");
            case "REIM", "REIMB" -> Optional.of("refund");
            case "ASSET" -> Optional.of("asset");
            case "LIABILITY" -> Optional.of("liability");
            case "INVEST", "WEALTH", "FP" -> Optional.of("investment");
            case "FE", "FEE" -> Optional.of("cashflow");
            case "FIXED", "LIVING", "SHOPPING", "TRANSPORT", "EDU", "ENT", "GIFT", "SOCIAL", "OTHER" ->
                    Optional.of("budget");
            default -> txn.contains("income") ? Optional.of("income")
                    : txn.contains("refund") ? Optional.of("refund")
                    : txn.contains("invest") ? Optional.of("investment")
                    : txn.contains("transfer") ? Optional.of("transfer")
                    : Optional.of("budget");
        };
    }
}
