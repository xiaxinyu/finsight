package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleRiskSupportTest {

    @Test
    void groupActiveByNormalizedPattern_isCaseInsensitive() {
        ConsumeRule a = activeRule("r1", "美团", "DAILY-01", 10);
        ConsumeRule b = activeRule("r2", " 美团 ", "DAILY-02", 20);
        ConsumeRule inactive = activeRule("r3", "美团", "DAILY-03", 5);
        inactive.setActive(0);

        Map<String, List<ConsumeRule>> groups = RuleRiskSupport.groupActiveByNormalizedPattern(
                List.of(a, b, inactive));

        assertEquals(1, groups.size());
        assertEquals(2, groups.get("美团").size());
    }

    @Test
    void analyzeRule_flagsDuplicateAndCrossCategoryConflict() {
        ConsumeCategory daily = category("c1", "DAILY-01", "expense");
        ConsumeCategory transport = category("c2", "TRANSPORT", "expense");
        Map<String, ConsumeCategory> byRef = RuleRiskSupport.indexCategoriesByRef(List.of(daily, transport));

        ConsumeRule r1 = activeRule("r1", "支付", "DAILY-01", 10);
        ConsumeRule r2 = activeRule("r2", "支付", "TRANSPORT", 20);
        Map<String, List<ConsumeRule>> groups = RuleRiskSupport.groupActiveByNormalizedPattern(List.of(r1, r2));

        Set<RuleRiskKind> risks = RuleRiskSupport.analyzeRule(
                r1, byRef, Set.of("c1", "c2"), Set.of("DAILY-01", "TRANSPORT"), groups);

        assertTrue(risks.contains(RuleRiskKind.DUPLICATE_PATTERN));
        assertTrue(risks.contains(RuleRiskKind.CROSS_CATEGORY_CONFLICT));
        assertTrue(risks.contains(RuleRiskKind.BROAD_KEYWORD));
    }

    @Test
    void analyzeRule_flagsDirectionMismatchForIncomeCategoryWithExpensePattern() {
        ConsumeCategory income = category("inc", "INCOME-01", "income");
        Map<String, ConsumeCategory> byRef = RuleRiskSupport.indexCategoriesByRef(List.of(income));

        ConsumeRule rule = activeRule("r1", "消费", "INCOME-01", 10);
        Map<String, List<ConsumeRule>> groups = RuleRiskSupport.groupActiveByNormalizedPattern(List.of(rule));

        Set<RuleRiskKind> risks = RuleRiskSupport.analyzeRule(
                rule, byRef, Set.of("inc"), Set.of("INCOME-01"), groups);

        assertTrue(risks.contains(RuleRiskKind.DIRECTION_MISMATCH));
        assertTrue(risks.contains(RuleRiskKind.BROAD_KEYWORD));
    }

    @Test
    void analyzeRule_flagsOrphanCategory() {
        ConsumeRule rule = activeRule("r1", "地铁", "GONE", 10);
        Map<String, List<ConsumeRule>> groups = RuleRiskSupport.groupActiveByNormalizedPattern(List.of(rule));

        Set<RuleRiskKind> risks = RuleRiskSupport.analyzeRule(
                rule, Map.of(), Set.of("c1"), Set.of("DAILY-01"), groups);

        assertTrue(risks.contains(RuleRiskKind.ORPHAN_CATEGORY));
        assertFalse(risks.contains(RuleRiskKind.DUPLICATE_PATTERN));
    }

    @Test
    void isHighRisk_whenAnyRiskPresent() {
        assertTrue(RuleRiskSupport.isHighRisk(Set.of(RuleRiskKind.DUPLICATE_PATTERN)));
        assertFalse(RuleRiskSupport.isHighRisk(Set.of()));
    }

    private static ConsumeRule activeRule(String id, String pattern, String categoryId, int priority) {
        ConsumeRule r = new ConsumeRule();
        r.setId(id);
        r.setPattern(pattern);
        r.setCategoryId(categoryId);
        r.setPriority(priority);
        r.setActive(1);
        return r;
    }

    private static ConsumeCategory category(String id, String code, String txnTypes) {
        ConsumeCategory c = new ConsumeCategory();
        c.setId(id);
        c.setCode(code);
        c.setTxnTypes(txnTypes);
        c.setDeleted(0);
        return c;
    }
}
