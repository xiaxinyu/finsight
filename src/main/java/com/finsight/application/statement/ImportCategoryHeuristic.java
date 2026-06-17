package com.finsight.application.statement;

import com.finsight.application.consume.ClassificationTextNormalizer;
import com.finsight.application.consume.ConsumeCategoryService;
import com.finsight.domain.model.ConsumeCategory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * High-confidence merchant patterns for statement import when DB rules misfire
 * (e.g. Tenpay metro fares classified as fund subscription).
 */
@Component
public class ImportCategoryHeuristic {

    public enum Family {
        TRANSIT, FOOD, INSTALLMENT, INVEST, OTHER
    }

    public record Match(Family family, String categoryCode, String categoryName, String reason) {
    }

    private record RuleGroup(Family family, String[] keywords, String[] categoryHints) {
    }

    private static final RuleGroup[] GROUPS = {
            new RuleGroup(Family.INSTALLMENT,
                    new String[]{"账单分期", "分期单期", "邮购分期"},
                    new String[]{"LOAN", "贷款", "分期", "还款", "INSTALL"}),
            new RuleGroup(Family.TRANSIT,
                    new String[]{"地铁", "深圳通", "公交", "曹操出行", "滴滴", "高德", "出租", "打车"},
                    new String[]{"TRAVEL", "交通", "出行", "TRANSPORT", "公共"}),
            new RuleGroup(Family.FOOD,
                    new String[]{"饮食", "餐饮", "美团", "饿了么", "外卖"},
                    new String[]{"FOOD", "EAT", "餐", "食"}),
            new RuleGroup(Family.INVEST,
                    new String[]{"基金", "申购", "赎回", "证券", "理财", "股票"},
                    new String[]{"INVEST", "投资", "基金"}),
    };

    private final ConsumeCategoryService categoryService;

    public ImportCategoryHeuristic(ConsumeCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public Optional<Match> match(String narration, double amount) {
        if (StringUtils.isBlank(narration)) {
            return Optional.empty();
        }
        String expanded = ClassificationTextNormalizer.expand(narration).toLowerCase(Locale.ROOT);
        for (RuleGroup group : GROUPS) {
            if (!containsAny(expanded, group.keywords())) {
                continue;
            }
            ConsumeCategory cat = findCategory(group.categoryHints());
            if (cat == null) {
                continue;
            }
            if (group.family() == Family.TRANSIT && amount > 0 && amount > 500) {
                continue;
            }
            return Optional.of(new Match(
                    group.family(),
                    cat.getCode(),
                    cat.getName(),
                    "Merchant pattern → " + cat.getName()));
        }
        return Optional.empty();
    }

    public boolean shouldOverrideRule(String ruleCode, String ruleName, Match heuristic, String narration) {
        if (heuristic == null || StringUtils.isBlank(narration)) {
            return false;
        }
        String n = narration.toLowerCase(Locale.ROOT);
        if (isInvestOrLoan(ruleCode, ruleName) && isDailySpend(heuristic.family())) {
            return hasDailySpendSignal(n) && !hasInvestSignal(n);
        }
        return false;
    }

    private static boolean isDailySpend(Family family) {
        return family == Family.TRANSIT || family == Family.FOOD;
    }

    private static boolean isInvestOrLoan(String code, String name) {
        String c = StringUtils.defaultString(code).toUpperCase(Locale.ROOT);
        String n = StringUtils.defaultString(name);
        if (c.startsWith("INVEST") || c.contains("LOAN") || c.contains("INSTALL")) {
            return true;
        }
        return n.contains("基金") || n.contains("申购") || n.contains("股票")
                || n.contains("贷款") || n.contains("分期") || n.contains("还款");
    }

    private static boolean hasDailySpendSignal(String narration) {
        return containsAny(narration,
                "地铁", "深圳通", "公交", "曹操出行", "滴滴", "高德", "出租", "打车", "交通",
                "饮食", "餐饮", "美团", "饿了么", "外卖");
    }

    private static boolean hasInvestSignal(String narration) {
        return containsAny(narration, "基金", "申购", "赎回", "证券", "理财", "股票");
    }

    private ConsumeCategory findCategory(String[] hints) {
        List<ConsumeCategory> all = categoryService.listAll();
        if (all == null || all.isEmpty()) {
            return null;
        }
        for (String hint : hints) {
            for (ConsumeCategory c : all) {
                if (c == null || isOtherCategory(c.getCode(), c.getName())) {
                    continue;
                }
                String code = StringUtils.defaultString(c.getCode()).toUpperCase(Locale.ROOT);
                String name = StringUtils.defaultString(c.getName());
                if (code.contains(hint.toUpperCase(Locale.ROOT)) || name.contains(hint)) {
                    return c;
                }
            }
        }
        return null;
    }

    private static boolean isOtherCategory(String code, String name) {
        String c = StringUtils.trimToEmpty(code).toUpperCase(Locale.ROOT);
        String n = StringUtils.trimToEmpty(name);
        return c.startsWith("OTHER") || "无法归类的支出".equals(n);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
