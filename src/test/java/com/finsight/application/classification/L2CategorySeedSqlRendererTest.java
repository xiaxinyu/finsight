package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class L2CategorySeedSqlRendererTest {

    @Test
    void rendersIdempotentInsertAndNameUpdates() {
        String sql = L2CategorySeedSqlRenderer.renderInsertStatements(java.util.Set.of())
                + L2CategorySeedSqlRenderer.renderNameUpdates()
                + L2CategorySeedSqlRenderer.renderReportRoleBackfill();

        assertTrue(sql.contains("where not exists"));
        assertTrue(sql.contains("DAILY-02"));
        assertTrue(sql.contains("report_role"));
        assertTrue(sql.contains("update cls_category set name"));
        assertTrue(!sql.contains("update cls_category set code"));
    }

    @Test
    void regenerateCommittedSeedScript() throws Exception {
        String body = L2CategorySeedSqlRenderer.renderInsertStatements(java.util.Set.of())
                + "\n"
                + L2CategorySeedSqlRenderer.renderNameUpdates()
                + "\n"
                + L2CategorySeedSqlRenderer.renderReportRoleBackfill();
        String header = """
                -- FinSight L2 category Sprint 2 seed (Issue #69).
                -- MANUAL ONLY — review before execution. Does NOT batch-update historical transactions.
                --
                --   mysql -u <user> -p finsight < docs/tech/database/l2-category-sprint2-seed.sql
                --
                -- Prerequisites: Flyway V23 (report_role column). Verify L1 parents exist or use Step 0 below.
                -- After apply: add rules in Rule Engine; do not bulk-recategorize txns without migration batch (#75).

                """;
        Path target = Path.of("docs/tech/database/l2-category-sprint2-seed.sql");
        Files.writeString(target, header + body, StandardCharsets.UTF_8);
        assertTrue(Files.size(target) > 1000);
    }
}
