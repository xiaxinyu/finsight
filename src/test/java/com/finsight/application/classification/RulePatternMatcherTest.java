package com.finsight.application.classification;

import com.finsight.domain.model.ClassificationRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulePatternMatcherTest {

    private final RulePatternMatcher matcher = new RulePatternMatcher();

    @Test
    void containsPattern_matchesNormalizedNarration() {
        ClassificationRule rule = rule("contains", "美团", null, null);
        assertTrue(matcher.matches(rule, "美团外卖订单", "", "", 28.0, null));
        assertFalse(matcher.matches(rule, "京东订单", "", "", 28.0, null));
    }

    @Test
    void equalsPattern_requiresExactNormalizedMatch() {
        ClassificationRule rule = rule("equals", "salary", null, null);
        assertTrue(matcher.matches(rule, "salary", "", "", 1000.0, null));
        assertFalse(matcher.matches(rule, "salary bonus", "", "", 1000.0, null));
    }

    @Test
    void respectsBankScope() {
        ClassificationRule rule = rule("contains", "地铁", "CMB", null);
        assertTrue(matcher.matches(rule, "地铁出行", "CMB", "", 5.0, null));
        assertFalse(matcher.matches(rule, "地铁出行", "CCB", "", 5.0, null));
    }

    private static ClassificationRule rule(String type, String pattern, String bank, String card) {
        ClassificationRule r = new ClassificationRule();
        r.setPatternType(type);
        r.setPattern(pattern);
        r.setBankCode(bank);
        r.setCardTypeCode(card);
        r.setActive(1);
        return r;
    }
}
