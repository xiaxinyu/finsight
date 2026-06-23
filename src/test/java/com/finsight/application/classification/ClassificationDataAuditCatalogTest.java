package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationDataAuditCatalogTest {

    private static final Path AUDIT_SQL = Path.of("docs/tech/database/classification-data-audit.sql");

    @Test
    void auditSqlContainsAllCatalogSections() throws IOException {
        String sql = Files.readString(AUDIT_SQL, StandardCharsets.UTF_8);
        for (ClassificationDataAuditCatalog section : ClassificationDataAuditCatalog.values()) {
            assertTrue(
                    sql.contains(section.sectionMarker()),
                    () -> "Missing audit section marker: " + section.sectionMarker() + " " + section.title());
        }
    }

    @Test
    void auditSqlLinksRemediationScripts() throws IOException {
        String sql = Files.readString(AUDIT_SQL, StandardCharsets.UTF_8);
        for (ClassificationDataAuditCatalog section : ClassificationDataAuditCatalog.remediationScripts()) {
            String script = section.remediationOrExport().orElseThrow();
            assertTrue(
                    sql.contains(script),
                    () -> "Section " + section.sectionNumber() + " should reference " + script);
        }
    }

    @Test
    void baselineExportsCoverSprint1Artifacts() {
        var exports = ClassificationDataAuditCatalog.baselineExports();
        assertTrue(exports.stream().anyMatch(s -> s.name().contains("UNCLASSIFIED")));
        assertTrue(exports.stream().anyMatch(s -> s.name().contains("OTHER")));
        assertTrue(exports.stream().anyMatch(s -> s.name().contains("SUMMARY")));
        assertTrue(exports.stream().anyMatch(s -> s.name().contains("MERCHANT")));
    }
}
