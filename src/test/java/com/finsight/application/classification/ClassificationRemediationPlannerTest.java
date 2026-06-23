package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationRemediationPlannerTest {

    @Test
    void prioritizesOrphansAndInvalidBeforeDuplicates() {
        ClassificationAuditSummary summary = new ClassificationAuditSummary(
                5, 2, 10, 100, 50, 3, 4, 1, 0, 0);

        List<ClassificationRemediationPlanner.RemediationItem> plan =
                ClassificationRemediationPlanner.buildPlan(summary);

        assertTrue(plan.size() >= 5);
        assertEquals(ClassificationRemediationPlanner.Priority.P0, plan.get(0).priority());
        assertTrue(plan.stream().anyMatch(i -> i.issue().contains("orphaned")));
        assertTrue(plan.stream().anyMatch(i -> i.issue().contains("Unclassified")));
        assertTrue(plan.stream().anyMatch(i -> i.priority() == ClassificationRemediationPlanner.Priority.P1
                && i.issue().contains("Duplicate")));
    }

    @Test
    void emptySummaryYieldsEmptyPlan() {
        assertTrue(ClassificationRemediationPlanner.buildPlan(ClassificationAuditSummary.empty()).isEmpty());
    }
}
