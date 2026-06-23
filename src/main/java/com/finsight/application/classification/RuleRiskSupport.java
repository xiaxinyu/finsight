package com.finsight.application.classification;

import com.finsight.domain.model.ConsumeCategory;
import com.finsight.domain.model.ConsumeRule;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure rule risk heuristics (audit §6 duplicate patterns, §7 broad keywords).
 */
public final class RuleRiskSupport {

    public static final Set<String> BROAD_KEYWORDS = Set.of(
            "支付", "消费", "转账", "付款", "收款", "交易", "代扣", "快捷", "微信", "支付宝");

    private static final String[] INCOME_PATTERN_HINTS = {
            "工资", "收入", "退款", "报销", "到账", "利息", "分红", "薪金", "奖金", "理财收益"
    };
    private static final String[] EXPENSE_PATTERN_HINTS = {
            "消费", "支付", "购买", "支出", "扣款", "付款", "代扣"
    };

    private RuleRiskSupport() {
    }

    public static String normalizePattern(String pattern) {
        return pattern == null ? "" : pattern.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAnalyzableActiveRule(ConsumeRule rule) {
        return rule != null
                && OrphanRuleSupport.isActive(rule)
                && StringUtils.isNotBlank(StringUtils.trimToNull(rule.getPattern()));
    }

    public static boolean isBroadKeyword(String pattern) {
        return BROAD_KEYWORDS.contains(normalizePattern(pattern));
    }

    public static boolean patternSuggestsIncome(String pattern) {
        return containsAnyHint(pattern, INCOME_PATTERN_HINTS);
    }

    public static boolean patternSuggestsExpense(String pattern) {
        return containsAnyHint(pattern, EXPENSE_PATTERN_HINTS);
    }

    public static boolean categoryAcceptsIncome(String txnTypes) {
        return containsTxnToken(txnTypes, "income");
    }

    public static boolean categoryAcceptsExpense(String txnTypes) {
        return containsTxnToken(txnTypes, "expense");
    }

    public static boolean isDirectionMismatch(String txnTypes, String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return false;
        }
        boolean incomeCat = categoryAcceptsIncome(txnTypes);
        boolean expenseCat = categoryAcceptsExpense(txnTypes);
        boolean incomePattern = patternSuggestsIncome(pattern);
        boolean expensePattern = patternSuggestsExpense(pattern);
        if (incomeCat && !expenseCat && expensePattern && !incomePattern) {
            return true;
        }
        return expenseCat && !incomeCat && incomePattern && !expensePattern;
    }

    public static String suggestAction(RuleRiskKind kind) {
        return switch (kind) {
            case DUPLICATE_PATTERN, CROSS_CATEGORY_CONFLICT ->
                    "Disable duplicate rules or adjust priority — keep one winner per pattern/category";
            case BROAD_KEYWORD ->
                    "Narrow keyword to a specific merchant or raise priority (lower number)";
            case DIRECTION_MISMATCH ->
                    "Remap category_id to a category with matching txn_types, or disable rule";
            case ORPHAN_CATEGORY ->
                    "Point category_id to an active code or disable rule";
            case INVALID_PATTERN ->
                    "Add a keyword or disable/delete blank-pattern rule";
            case NO_CATEGORY ->
                    "Assign category_id to a valid active code";
        };
    }

    public static boolean isHighRisk(Set<RuleRiskKind> risks) {
        return risks != null && !risks.isEmpty();
    }

    public static Map<String, List<ConsumeRule>> groupActiveByNormalizedPattern(List<ConsumeRule> rules) {
        Map<String, List<ConsumeRule>> groups = new LinkedHashMap<>();
        for (ConsumeRule rule : rules) {
            if (!isAnalyzableActiveRule(rule)) {
                continue;
            }
            String key = normalizePattern(rule.getPattern());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
        }
        groups.values().forEach(list -> list.sort(Comparator
                .comparingInt((ConsumeRule r) -> r.getPriority() == null ? 999 : r.getPriority())
                .thenComparing(r -> StringUtils.defaultString(r.getId()))));
        return groups;
    }

    public static Set<RuleRiskKind> analyzeRule(
            ConsumeRule rule,
            Map<String, ConsumeCategory> categoryByRef,
            Set<String> activeCategoryIds,
            Set<String> activeCodes,
            Map<String, List<ConsumeRule>> patternGroups) {
        Set<RuleRiskKind> risks = new LinkedHashSet<>();
        String pattern = rule == null ? null : rule.getPattern();
        if (StringUtils.isBlank(StringUtils.trimToNull(pattern))) {
            if (InvalidRuleSupport.isActiveInvalidPattern(rule)) {
                risks.add(RuleRiskKind.INVALID_PATTERN);
            }
            return risks;
        }

        String catId = StringUtils.trimToEmpty(rule.getCategoryId());
        if (catId.isEmpty()) {
            risks.add(RuleRiskKind.NO_CATEGORY);
            return risks;
        }

        if (OrphanRuleSupport.isActiveOrphan(rule, activeCategoryIds, activeCodes)) {
            risks.add(RuleRiskKind.ORPHAN_CATEGORY);
        }

        if (!isAnalyzableActiveRule(rule)) {
            return risks;
        }

        if (isBroadKeyword(pattern)) {
            risks.add(RuleRiskKind.BROAD_KEYWORD);
        }

        ConsumeCategory cat = resolveCategory(catId, categoryByRef);
        if (cat != null && isDirectionMismatch(cat.getTxnTypes(), pattern)) {
            risks.add(RuleRiskKind.DIRECTION_MISMATCH);
        }

        List<ConsumeRule> peers = patternGroups.get(normalizePattern(pattern));
        if (peers != null && peers.size() > 1) {
            risks.add(RuleRiskKind.DUPLICATE_PATTERN);
            Set<String> categories = new LinkedHashSet<>();
            for (ConsumeRule peer : peers) {
                if (StringUtils.isNotBlank(peer.getCategoryId())) {
                    categories.add(peer.getCategoryId().trim());
                }
            }
            if (categories.size() > 1) {
                risks.add(RuleRiskKind.CROSS_CATEGORY_CONFLICT);
            }
        }
        return risks;
    }

    public static ConsumeCategory resolveCategory(String categoryRef, Map<String, ConsumeCategory> categoryByRef) {
        if (categoryRef == null || categoryByRef == null) {
            return null;
        }
        return categoryByRef.get(categoryRef.trim());
    }

    public static Map<String, ConsumeCategory> indexCategoriesByRef(List<ConsumeCategory> categories) {
        Map<String, ConsumeCategory> out = new LinkedHashMap<>();
        for (ConsumeCategory cat : categories) {
            if (cat == null) {
                continue;
            }
            if (StringUtils.isNotBlank(cat.getCode())) {
                out.put(cat.getCode().trim(), cat);
            }
            if (StringUtils.isNotBlank(cat.getId())) {
                out.put(cat.getId().trim(), cat);
            }
        }
        return out;
    }

    private static boolean containsTxnToken(String txnTypes, String token) {
        if (StringUtils.isBlank(txnTypes) || StringUtils.isBlank(token)) {
            return false;
        }
        for (String part : txnTypes.toLowerCase(Locale.ROOT).split(",")) {
            if (token.equalsIgnoreCase(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyHint(String pattern, String[] hints) {
        if (StringUtils.isBlank(pattern)) {
            return false;
        }
        String p = pattern.toLowerCase(Locale.ROOT);
        for (String hint : hints) {
            if (p.contains(hint)) {
                return true;
            }
        }
        return false;
    }
}
