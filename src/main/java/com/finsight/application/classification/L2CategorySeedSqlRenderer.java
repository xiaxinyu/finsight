package com.finsight.application.classification;

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
            sb.append("update cls_category set report_role = '")
                    .append(escapeSql(target.reportRole()))
                    .append("' where code = '")
                    .append(escapeSql(target.code()))
                    .append("' and coalesce(deleted, 0) = 0")
                    .append(" and (report_role is null or trim(report_role) = '');\n");
        }
        return sb.toString();
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
        sb.append("-- Skip INCOME L1 when legacy INC already exists\n");
        for (ClassificationL1TargetCatalog l1 : ClassificationL1TargetCatalog.all()) {
            if ("INCOME".equals(l1.code()) && existingCodes != null && existingCodes.contains("INC")) {
                sb.append("-- skipped INCOME L1 (legacy INC present)\n");
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
}
