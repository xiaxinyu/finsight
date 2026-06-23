package com.finsight.application.classification;

/**
 * Aggregated metrics from classification-data-audit.sql §20 and related exports.
 */
public record ClassificationAuditSummary(
        long activeOrphanRules,
        long activeInvalidPatternRules,
        long categoryFieldDriftRows,
        long unclassifiedTxns,
        long otherCategoryTxns,
        long merchantProfileMismatchCount,
        long duplicatePatternGroups,
        long broadKeywordRules,
        long rulesWithoutCategory,
        long txnMissingCategoryGroups) {

    public static ClassificationAuditSummary empty() {
        return new ClassificationAuditSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
