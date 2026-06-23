package com.finsight.application.classification;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Canonical sections in {@code docs/tech/database/classification-data-audit.sql}.
 * Aligns with v1.8 plan §7 and Sprint 1 baseline exports.
 */
public enum ClassificationDataAuditCatalog {

    CATEGORY_TREE(1, "Category tree health", null),
    CATEGORY_ORPHAN_PARENT(2, "Active child categories without parent", null),
    ORPHANED_RULES(3, "Orphaned rules", "orphan-rules-remediation.sql"),
    INVALID_RULES(4, "Invalid rules with blank pattern", "invalid-rules-remediation.sql"),
    RULES_NO_CATEGORY(5, "Rules without category", null),
    DUPLICATE_PATTERNS(6, "Duplicate active rule patterns", null),
    BROAD_KEYWORDS(7, "Broad keyword risk", null),
    RULE_COUNT_BY_CATEGORY(8, "Rule count by category", null),
    UNCLASSIFIED_COVERAGE(9, "Transaction classification coverage", null),
    TXN_MISSING_CATEGORY(10, "Transactions pointing to missing or deleted categories", null),
    CATEGORY_FIELD_DRIFT(11, "Transaction category field drift", "transaction-category-field-remediation.sql"),
    REPORT_CATEGORY_AMOUNTS(12, "Category amount by level (analytics view)", null),
    MONTHLY_INCOME_EXPENSE(13, "Monthly income / expense sanity check", null),
    MERCHANT_TOKEN_COVERAGE(14, "Merchant token coverage", "merchant-token-normalization.sql"),
    MERCHANT_PROFILE_MISMATCH(15, "Merchant profile token mismatch", null),
    FIXED_VARIABLE_SANITY(16, "Fixed vs variable sanity", null),
    TRANSFER_REFUND_VOLUME(17, "Transfer and refund exclusion volume", null),
    UNCLASSIFIED_TOP100(18, "Top unclassified raw descriptions", "baseline-unclassified-top100.csv"),
    OTHER_CONSUMPTION_TOP100(19, "Top OTHER / catch-all category transactions", "baseline-other-consumption-top100.csv"),
    BASELINE_SUMMARY(20, "Audit baseline summary counts", "baseline-summary.json"),
    MERCHANT_TOKEN_SAMPLES(21, "Merchant token normalization samples", "baseline-merchant-token-samples.csv");

    private final int sectionNumber;
    private final String title;
    private final String remediationOrExport;

    ClassificationDataAuditCatalog(int sectionNumber, String title, String remediationOrExport) {
        this.sectionNumber = sectionNumber;
        this.title = title;
        this.remediationOrExport = remediationOrExport;
    }

    public int sectionNumber() {
        return sectionNumber;
    }

    public String title() {
        return title;
    }

    public Optional<String> remediationOrExport() {
        return Optional.ofNullable(remediationOrExport);
    }

    public String sectionMarker() {
        return "-- " + sectionNumber + ".";
    }

    public static List<ClassificationDataAuditCatalog> baselineExports() {
        return Arrays.stream(values())
                .filter(s -> s.remediationOrExport != null && s.remediationOrExport.startsWith("baseline-"))
                .toList();
    }

    public static List<ClassificationDataAuditCatalog> remediationScripts() {
        return Arrays.stream(values())
                .filter(s -> s.remediationOrExport != null && s.remediationOrExport.endsWith(".sql"))
                .toList();
    }
}
