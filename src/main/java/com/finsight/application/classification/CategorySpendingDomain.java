package com.finsight.application.classification;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Optional;

/** Maps category tree position + name to expense domain semantic tags. */
public final class CategorySpendingDomain {

    private CategorySpendingDomain() {
    }

    public static Optional<String> inferDomainTag(String code, String parentId, String name) {
        String c = StringUtils.trimToEmpty(code).toUpperCase(Locale.ROOT);
        String parent = StringUtils.trimToEmpty(parentId).toUpperCase(Locale.ROOT);
        String n = StringUtils.defaultString(name);

        if (matchesShopping(c, parent, n)) {
            return Optional.of("shopping_spending");
        }
        if (matchesTransport(c, parent, n)) {
            return Optional.of("transport_spending");
        }
        if (matchesDining(c, parent, n)) {
            return Optional.of("dining_spending");
        }
        if (matchesEntertainment(c, parent, n)) {
            return Optional.of("entertainment_spending");
        }
        if (matchesEducation(c, parent, n)) {
            return Optional.of("education_spending");
        }
        return Optional.empty();
    }

    static boolean matchesDining(String code, String parent, String name) {
        if (code.startsWith("DAILY-01") || code.startsWith("DAILY-02")) {
            return true;
        }
        return containsAny(name, "餐饮", "外卖", "堂食", "早餐", "咖啡", "饭店", "吃饭", "小吃");
    }

    static boolean matchesShopping(String code, String parent, String name) {
        if (parent.equals("SHOPPING") || code.startsWith("SHOP-") || code.startsWith("SHOPPING-")) {
            return true;
        }
        if (code.equals("DAILY-03") || code.equals("DAILY-04")) {
            return true;
        }
        return containsAny(name, "超市", "购物", "网上", "电商", "服饰", "美妆", "母婴", "家居", "家装",
                "耐用品", "日用品", "百货");
    }

    static boolean matchesTransport(String code, String parent, String name) {
        if (parent.equals("TRANSPORT") || parent.equals("TRAVEL")
                || code.startsWith("TRANS-") || code.startsWith("TRAVEL-") || code.startsWith("TRANSPORT-")) {
            if (code.equals("TRANS-06") || name.contains("保险")) {
                return false;
            }
            return true;
        }
        return containsAny(name, "交通", "地铁", "公交", "打车", "网约车", "滴滴", "停车", "油费", "充电",
                "过路", "过桥", "车辆", "机票", "火车", "租车", "代驾", "保养", "洗车");
    }

    static boolean matchesEntertainment(String code, String parent, String name) {
        if (parent.equals("ENT") || code.startsWith("ENT-")) {
            return true;
        }
        return containsAny(name, "娱乐", "旅行", "旅游", "酒店", "景点", "门票", "电影", "演出", "游戏", "健身", "运动");
    }

    static boolean matchesEducation(String code, String parent, String name) {
        if (parent.equals("EDU") || code.startsWith("EDU-")) {
            if (code.equals("EDU-01") || name.contains("学费")) {
                return false;
            }
            return true;
        }
        return containsAny(name, "培训", "书籍", "资料", "课程");
    }

    private static boolean containsAny(String text, String... hints) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        for (String hint : hints) {
            if (text.contains(hint)) {
                return true;
            }
        }
        return false;
    }
}
