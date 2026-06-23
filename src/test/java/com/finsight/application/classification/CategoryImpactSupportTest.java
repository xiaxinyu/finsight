package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryImpactSupportTest {

    @Test
    void categoryRefsIncludesCodeAndId() {
        com.finsight.domain.model.ConsumeCategory cat = new com.finsight.domain.model.ConsumeCategory();
        cat.setCode("DAILY-01");
        cat.setId("DAILY-01");

        List<String> refs = CategoryImpactSupport.categoryRefs(cat);

        assertEquals(1, refs.size());
        assertEquals("DAILY-01", refs.get(0));
    }

    @Test
    void transactionMatchSqlBuildsOrClauses() {
        String sql = CategoryImpactSupport.transactionMatchSql(List.of("A", "B"));
        assertTrue(sql.contains("consume_code = ?"));
        assertTrue(sql.contains("category_id = ?"));
        assertEquals(8, CategoryImpactSupport.transactionMatchParams(List.of("A", "B")).length);
    }

    @Test
    void deleteWarningsCoverTransactionsAndRules() {
        List<String> warnings = CategoryImpactSupport.warningsFor(
                CategoryImpactAction.DELETE, 0, 12, 3, null);
        assertFalse(warnings.isEmpty());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("12 transactions")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("3 active rules")));
    }

    @Test
    void mergeRequiresTargetCodeWarning() {
        List<String> warnings = CategoryImpactSupport.warningsFor(
                CategoryImpactAction.MERGE, 0, 5, 1, null);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("target category code")));
    }
}
