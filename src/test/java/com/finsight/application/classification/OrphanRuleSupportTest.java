package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeRule;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrphanRuleSupportTest {

    @Test
    void inactiveLegacyOrphanIsNotActiveOrphan() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCategoryId("OLD-CODE");
        rule.setActive(0);
        rule.setRemark("migrated " + OrphanRuleSupport.LEGACY_ORPHAN_REMARK);

        assertTrue(OrphanRuleSupport.isLegacyArchived(rule));
        assertFalse(OrphanRuleSupport.isActiveOrphan(rule, Set.of("DAILY-01"), Set.of("DAILY-01")));
    }

    @Test
    void activeRuleWithMissingCategoryIsActiveOrphan() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCategoryId("MISSING");
        rule.setActive(1);

        assertTrue(OrphanRuleSupport.isActiveOrphan(rule, Set.of("id-1"), Set.of("DAILY-01")));
    }

    @Test
    void activeRuleLinkedByCodeIsNotOrphan() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCategoryId("DAILY-01");
        rule.setActive(1);

        assertFalse(OrphanRuleSupport.isActiveOrphan(rule, Set.of("other-id"), Set.of("DAILY-01")));
    }

    @Test
    void recognizesAutoDisabledRemarkFromPriorMigration() {
        ConsumeRule rule = new ConsumeRule();
        rule.setCategoryId("OLD");
        rule.setActive(0);
        rule.setRemark("note " + OrphanRuleSupport.AUTO_DISABLED_ORPHAN_REMARK);

        assertTrue(OrphanRuleSupport.isLegacyArchived(rule));
    }
}
