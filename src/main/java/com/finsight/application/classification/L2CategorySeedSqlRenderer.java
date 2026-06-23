package com.finsight.application.classification;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * Renders idempotent manual SQL for {@link ClassificationL2TargetCatalog} inserts.
 */
public final class L2CategorySeedSqlRenderer {

    private L2CategorySeedSqlRenderer() {
    }

    public static String renderInsertStatements(Set<String> existingCodes) {
        L2CategorySeedPlanner.validateCatalog();
        StringBuilder sb = new StringBuilder();
        sb.append("-- Generated from ClassificationL2TargetCatalog — Issue #69\n");
        sb.append("-- Manual execution only; never auto-applied by Flyway.\n\n");
        sb.append(renderL1Ensures(existingCodes)).append("\n");
        for (L2CategorySeedPlanner.SeedItem item : L2CategorySeedPlanner.buildInsertPlan(existingCodes)) {
            if (item.action() != L2CategorySeedPlanner.Action.INSERT) {
                continue;
            }
            sb.append(renderInsert(item)).append("\n");
        }
        return sb.toString();
    }

    public static String renderNameUpdates() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Optional name clarifications (does NOT change code)\n\n");
        for (L2CategorySeedPlanner.NameUpdate update : L2CategorySeedPlanner.buildNameUpdates()) {
            sb.append("update cls_category set name = '")
                    .append(escapeSql(update.newName()))
                    .append("', updated_at = now() where code = '")
                    .append(escapeSql(update.code()))
                    .append("' and coalesce(deleted, 0) = 0");
            if (update.previousNameHint() != null && !update.previousNameHint().isBlank()) {
                sb.append(" and name = '").append(escapeSql(update.previousNameHint())).append("'");
            }
            if (update.level() != null) {
                sb.append(" and level = ").append(update.level());
            }
            sb.append(";\n");
        }
        return sb.toString();
    }

    public static String renderReportRoleBackfill() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Backfill report_role for catalog codes (requires V23 column)\n\n");
        for (ClassificationL2TargetCatalog target : ClassificationL2TargetCatalog.values()) {
            sb.append(renderReportRoleUpdate(target.code(), target.reportRole()));
        }
        return sb.toString();
    }

    /**
     * Backfill report_role for rows loaded from live DB (only where role is currently empty).
     */
    public static String renderReportRoleBackfillFromDatabase(java.util.List<CategoryReportRoleInference.DbCategoryRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Backfill report_role for live cls_category rows (inferred; requires V23 column)\n\n");
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (CategoryReportRoleInference.DbCategoryRow row : rows) {
            if (row == null || StringUtils.isBlank(row.code()) || !seen.add(row.code().trim())) {
                continue;
            }
            CategoryReportRoleInference.inferReportRole(row)
                    .ifPresent(role -> sb.append(renderReportRoleUpdate(row.code(), role)));
        }
        return sb.toString();
    }

    private static String renderReportRoleUpdate(String code, String reportRole) {
        return "update cls_category set report_role = '"
                + escapeSql(reportRole)
                + "' where code = '"
                + escapeSql(code)
                + "' and coalesce(deleted, 0) = 0"
                + " and (report_role is null or trim(report_role) = '');\n";
    }

    private static String renderInsert(L2CategorySeedPlanner.SeedItem item) {
        return "insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, report_role, "
                + "deleted, version, created_at, updated_at) "
                + "select '"
                + escapeSql(item.code()) + "', '"
                + escapeSql(item.code()) + "', '"
                + escapeSql(item.name()) + "', 2, '"
                + escapeSql(item.parentL1Code()) + "', "
                + item.sortNo() + ", '"
                + escapeSql(item.txnTypes()) + "', '"
                + escapeSql(item.reportRole()) + "', "
                + "0, 0, now(), now() "
                + "from dual where not exists ("
                + "select 1 from cls_category where code = '"
                + escapeSql(item.code()) + "');";
    }

    private static String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private static String renderL1Ensures(Set<String> existingCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Step 0: ensure L1 roots exist (insert if missing)\n");
        sb.append("-- Skips duplicate L1 when canonical root already exists (INC, TRANSPORT)\n");
        for (ClassificationL1TargetCatalog l1 : ClassificationL1TargetCatalog.all()) {
            if (shouldSkipL1Ensure(l1.code(), existingCodes)) {
                sb.append("-- skipped ").append(l1.code()).append(" L1 (canonical root already present)\n");
                continue;
            }
            sb.append("insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, "
                    + "deleted, version, created_at, updated_at) select '")
                    .append(escapeSql(l1.code())).append("', '")
                    .append(escapeSql(l1.code())).append("', '")
                    .append(escapeSql(l1.displayName())).append("', 1, null, ")
                    .append(l1.sortNo()).append(", '")
                    .append(escapeSql(l1.txnTypes())).append("', 0, 0, now(), now() from dual where not exists (")
                    .append("select 1 from cls_category where code = '")
                    .append(escapeSql(l1.code())).append("');\n");
        }
        return sb.toString();
    }

    private static boolean shouldSkipL1Ensure(String l1Code, Set<String> existingCodes) {
        if (existingCodes == null || l1Code == null) {
            return false;
        }
        if ("INC".equals(l1Code) && existingCodes.contains("INCOME")) {
            return false;
        }
        if ("INCOME".equals(l1Code) && existingCodes.contains("INC")) {
            return true;
        }
        if ("TRANSPORT".equals(l1Code) && existingCodes.contains("TRAVEL")) {
            return false;
        }
        if ("TRAVEL".equals(l1Code) && existingCodes.contains("TRANSPORT")) {
            return true;
        }
        return false;
    }
}
