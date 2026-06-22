package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeRule;
import org.apache.commons.lang3.StringUtils;

/**
 * Shared invalid-pattern (blank keyword) rule semantics for hygiene API and Rule Engine UI parity.
 */
public final class InvalidRuleSupport {

    /** From {@code V15__classification_rule_hygiene.sql} auto-archive migration. */
    public static final String AUTO_DISABLED_BLANK_PATTERN_REMARK = "[auto-disabled: blank pattern]";
    /** Manual remediation marker ({@code invalid-rules-remediation.sql}). */
    public static final String INACTIVE_LEGACY_BLANK_PATTERN_REMARK = "[inactive legacy: blank pattern]";

    private InvalidRuleSupport() {
    }

    public static boolean isBlankPattern(ConsumeRule rule) {
        if (rule == null) {
            return true;
        }
        return StringUtils.isBlank(rule.getPattern());
    }

    public static boolean isArchivedInvalidPattern(ConsumeRule rule) {
        if (rule == null || !isBlankPattern(rule)) {
            return false;
        }
        if (OrphanRuleSupport.isActive(rule)) {
            return false;
        }
        String remark = StringUtils.defaultString(rule.getRemark()).toLowerCase();
        return remark.contains("[auto-disabled: blank pattern]")
                || remark.contains("[inactive legacy: blank pattern]");
    }

    /** Active blank-pattern rules that still need remediation. */
    public static boolean isActiveInvalidPattern(ConsumeRule rule) {
        return rule != null && isBlankPattern(rule) && OrphanRuleSupport.isActive(rule);
    }

    /** Inactive blank-pattern rows missing an archive remark (audit gap). */
    public static boolean isInactiveInvalidWithoutRemark(ConsumeRule rule) {
        if (rule == null || !isBlankPattern(rule) || OrphanRuleSupport.isActive(rule)) {
            return false;
        }
        return !isArchivedInvalidPattern(rule);
    }
}
