package com.finsight.application.classification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds prioritized rule/category remediation tasks from audit summary metrics.
 */
public final class ClassificationRemediationPlanner {

    public enum Priority {
        P0, P1, P2
    }

    public record RemediationItem(
            Priority priority,
            String area,
            String issue,
            String recommendedAction,
            String remediationRef,
            long countHint) {
    }

    private ClassificationRemediationPlanner() {
    }

    public static List<RemediationItem> buildPlan(ClassificationAuditSummary summary) {
        List<RemediationItem> items = new ArrayList<>();
        if (summary == null) {
            return items;
        }

        if (summary.activeOrphanRules() > 0) {
            items.add(new RemediationItem(
                    Priority.P0,
                    "rules",
                    "Active orphaned rules",
                    "Remap to active category codes or archive with remark",
                    "docs/tech/database/orphan-rules-remediation.sql",
                    summary.activeOrphanRules()));
        }
        if (summary.activeInvalidPatternRules() > 0) {
            items.add(new RemediationItem(
                    Priority.P0,
                    "rules",
                    "Active invalid (blank pattern) rules",
                    "Archive or restore keyword",
                    "docs/tech/database/invalid-rules-remediation.sql",
                    summary.activeInvalidPatternRules()));
        }
        if (summary.categoryFieldDriftRows() > 0) {
            items.add(new RemediationItem(
                    Priority.P0,
                    "transactions",
                    "Category field drift vs consume_code",
                    "Sync consume_id/name and category_* from cls_category",
                    "docs/tech/database/transaction-category-field-remediation.sql",
                    summary.categoryFieldDriftRows()));
        }
        if (summary.merchantProfileMismatchCount() > 0) {
            items.add(new RemediationItem(
                    Priority.P0,
                    "merchant",
                    "Merchant profile tokens with no analytics match",
                    "Align merchant token normalization",
                    "docs/tech/database/merchant-token-normalization.sql",
                    summary.merchantProfileMismatchCount()));
        }
        if (summary.duplicatePatternGroups() > 0) {
            items.add(new RemediationItem(
                    Priority.P1,
                    "rules",
                    "Duplicate active rule patterns",
                    "Consolidate or deactivate conflicting rules",
                    "Rule Engine — review duplicate patterns export",
                    summary.duplicatePatternGroups()));
        }
        if (summary.broadKeywordRules() > 0) {
            items.add(new RemediationItem(
                    Priority.P1,
                    "rules",
                    "Overly broad keywords",
                    "Narrow patterns or raise priority selectively",
                    "Rule Engine — review broad keyword export",
                    summary.broadKeywordRules()));
        }
        if (summary.unclassifiedTxns() > 0) {
            items.add(new RemediationItem(
                    Priority.P1,
                    "classification",
                    "Unclassified transactions",
                    "Add rules from unclassified Top100 export",
                    "baseline-unclassified-top100.csv",
                    summary.unclassifiedTxns()));
        }
        if (summary.otherCategoryTxns() > 0) {
            items.add(new RemediationItem(
                    Priority.P1,
                    "classification",
                    "OTHER / catch-all category volume",
                    "Split into L2 categories per l2-category-candidates.zh-cn.md",
                    "baseline-other-consumption-top100.csv",
                    summary.otherCategoryTxns()));
        }
        if (summary.rulesWithoutCategory() > 0) {
            items.add(new RemediationItem(
                    Priority.P2,
                    "rules",
                    "Rules without category",
                    "Assign active category code or archive",
                    "classification-data-audit.sql §5",
                    summary.rulesWithoutCategory()));
        }
        if (summary.txnMissingCategoryGroups() > 0) {
            items.add(new RemediationItem(
                    Priority.P2,
                    "transactions",
                    "Transactions with deleted/missing category codes",
                    "Remap consume_code or leave documented exception",
                    "classification-data-audit.sql §10",
                    summary.txnMissingCategoryGroups()));
        }

        items.sort(Comparator.comparing(RemediationItem::priority).thenComparing(RemediationItem::area));
        return items;
    }
}
