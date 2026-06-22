package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvalidRuleSupportTest {

    @Test
    void activeBlankPatternIsActiveInvalid() {
        ConsumeRule rule = new ConsumeRule();
        rule.setPattern("  ");
        rule.setActive(1);

        assertTrue(InvalidRuleSupport.isBlankPattern(rule));
        assertTrue(InvalidRuleSupport.isActiveInvalidPattern(rule));
        assertFalse(InvalidRuleSupport.isArchivedInvalidPattern(rule));
    }

    @Test
    void autoDisabledBlankPatternIsArchivedInvalid() {
        ConsumeRule rule = new ConsumeRule();
        rule.setPattern(null);
        rule.setActive(0);
        rule.setRemark("note " + InvalidRuleSupport.AUTO_DISABLED_BLANK_PATTERN_REMARK);

        assertTrue(InvalidRuleSupport.isArchivedInvalidPattern(rule));
        assertFalse(InvalidRuleSupport.isActiveInvalidPattern(rule));
        assertFalse(InvalidRuleSupport.isInactiveInvalidWithoutRemark(rule));
    }

    @Test
    void inactiveBlankWithoutRemarkNeedsBackfill() {
        ConsumeRule rule = new ConsumeRule();
        rule.setPattern("");
        rule.setActive(0);

        assertTrue(InvalidRuleSupport.isInactiveInvalidWithoutRemark(rule));
        assertFalse(InvalidRuleSupport.isArchivedInvalidPattern(rule));
    }

    @Test
    void healthyRuleIsNotInvalid() {
        ConsumeRule rule = new ConsumeRule();
        rule.setPattern("地铁");
        rule.setActive(1);

        assertFalse(InvalidRuleSupport.isBlankPattern(rule));
        assertFalse(InvalidRuleSupport.isActiveInvalidPattern(rule));
    }
}
