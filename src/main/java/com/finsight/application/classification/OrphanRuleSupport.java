package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared orphan-rule semantics for hygiene API, maintenance checks, and Rule Engine UI parity.
 */
public final class OrphanRuleSupport {

    public static final String LEGACY_ORPHAN_REMARK = "[inactive legacy: orphan category]";
    public static final String AUTO_DISABLED_ORPHAN_REMARK = "[auto-disabled: orphan category]";

    private OrphanRuleSupport() {
    }

    public static boolean isActive(ConsumeRule rule) {
        if (rule == null) {
            return false;
        }
        Integer active = rule.getActive();
        return active == null || active != 0;
    }

    public static boolean isLegacyArchived(ConsumeRule rule) {
        if (rule == null || isActive(rule)) {
            return false;
        }
        String remark = StringUtils.defaultString(rule.getRemark()).toLowerCase();
        return remark.contains("[inactive legacy:")
                || remark.contains("[auto-disabled: orphan");
    }

    public static boolean pointsToActiveCategory(ConsumeRule rule, Set<String> activeCategoryIds, Set<String> activeCodes) {
        String catId = StringUtils.trimToEmpty(rule == null ? null : rule.getCategoryId());
        if (catId.isEmpty()) {
            return false;
        }
        return activeCategoryIds.contains(catId) || activeCodes.contains(catId);
    }

    public static boolean isActiveOrphan(ConsumeRule rule, Set<String> activeCategoryIds, Set<String> activeCodes) {
        if (rule == null || !isActive(rule) || isLegacyArchived(rule)) {
            return false;
        }
        String catId = StringUtils.trimToEmpty(rule.getCategoryId());
        if (catId.isEmpty()) {
            return false;
        }
        return !pointsToActiveCategory(rule, activeCategoryIds, activeCodes);
    }

    public static Set<String> activeCategoryIds(Collection<ConsumeCategory> activeCategories) {
        return activeCategories.stream()
                .map(ConsumeCategory::getId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    public static Set<String> activeCategoryCodes(Collection<ConsumeCategory> activeCategories) {
        return activeCategories.stream()
                .flatMap(c -> Stream.of(c.getCode()))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
