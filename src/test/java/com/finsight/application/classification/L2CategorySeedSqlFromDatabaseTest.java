package com.finsight.application.classification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regenerates {@code docs/tech/database/l2-category-sprint2-seed.sql} from live DB codes.
 * Run: mvn test -Dtest=L2CategorySeedSqlFromDatabaseTest -Dregenerate.seed.from.db=true
 */
class L2CategorySeedSqlFromDatabaseTest {

    @Test
    @EnabledIfSystemProperty(named = "regenerate.seed.from.db", matches = "true")
    void regenerateFromDatabase() throws Exception {
        Set<String> existing = loadExistingCodes();
        List<CategoryReportRoleInference.DbCategoryRow> rows = loadActiveCategoryRows();
        String body = L2CategorySeedSqlRenderer.renderInsertStatements(existing)
                + "\n"
                + L2CategorySeedSqlRenderer.renderNameUpdates()
                + "\n"
                + L2CategorySeedSqlRenderer.renderReportRoleBackfillFromDatabase(rows);
        long insertCount = body.lines().filter(l -> l.startsWith("insert into cls_category")).count();
        long roleCount = body.lines().filter(l -> l.startsWith("update cls_category set report_role")).count();
        String header = """
                -- FinSight L2 category Sprint 2 seed (Issue #69).
                -- MANUAL ONLY — review before execution. Does NOT batch-update historical transactions.
                --
                --   mysql -u <user> -p finsight < docs/tech/database/l2-category-sprint2-seed.sql
                --
                -- Generated from live cls_category (%d distinct codes, %d active rows scanned).
                -- Inserts: %d idempotent L1/L2 rows still missing from catalog.
                -- report_role updates: %d rows (inferred for empty report_role only).
                -- Prerequisites: Flyway V23+ (report_role). V24+ optional (budgetable/cashflow_impact).
                -- After apply: add rules in Rule Engine; do not bulk-recategorize txns without migration batch (#75).
                -- Regenerate: mvn test -Dtest=L2CategorySeedSqlFromDatabaseTest -Dregenerate.seed.from.db=true

                """.formatted(existing.size(), rows.size(), insertCount, roleCount);
        Path target = Path.of("docs/tech/database/l2-category-sprint2-seed.sql");
        Files.writeString(target, header + body, StandardCharsets.UTF_8);
    }

    @Test
    @EnabledIfSystemProperty(named = "dump.category.codes", matches = "true")
    void dumpDatabaseCodes() throws Exception {
        try (Connection conn = openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "select code, name, level, parent_id, sort_no, txn_types, report_role, deleted "
                             + "from cls_category where coalesce(deleted,0)=0 "
                             + "order by level, parent_id, sort_no, code")) {
            while (rs.next()) {
                System.out.printf("%s\tL%d\tparent=%s\trole=%s\t%s%n",
                        rs.getString("code"),
                        rs.getInt("level"),
                        rs.getString("parent_id"),
                        rs.getString("report_role"),
                        rs.getString("name"));
            }
        }
    }

    private static Set<String> loadExistingCodes() throws Exception {
        Set<String> codes = new HashSet<>();
        try (Connection conn = openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "select distinct code from cls_category where code is not null and trim(code) <> ''")) {
            while (rs.next()) {
                String code = rs.getString(1);
                if (code != null && !code.isBlank()) {
                    codes.add(code.trim());
                }
            }
        }
        return codes;
    }

    private static List<CategoryReportRoleInference.DbCategoryRow> loadActiveCategoryRows() throws Exception {
        List<CategoryReportRoleInference.DbCategoryRow> rows = new ArrayList<>();
        try (Connection conn = openConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "select code, name, level, parent_id, txn_types from cls_category "
                             + "where coalesce(deleted,0)=0 order by level, parent_id, sort_no, code")) {
            while (rs.next()) {
                rows.add(new CategoryReportRoleInference.DbCategoryRow(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("level"),
                        rs.getString("parent_id"),
                        rs.getString("txn_types")));
            }
        }
        return rows;
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(jdbcUrl(), jdbcUser(), jdbcPass());
    }

    private static String jdbcUrl() {
        return System.getenv().getOrDefault(
                "SPRING_DATASOURCE_URL",
                "jdbc:mysql://127.0.0.1:3306/finsight?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    }

    private static String jdbcUser() {
        return System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "root");
    }

    private static String jdbcPass() {
        return System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "123456");
    }
}
