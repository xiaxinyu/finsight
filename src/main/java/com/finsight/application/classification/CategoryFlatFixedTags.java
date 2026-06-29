package com.finsight.application.classification;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Map;

/** Flat fixed-cost semantic tags — one tag per obligation type (no secondary picker). */
public final class CategoryFlatFixedTags {

    private static final Map<String, String> CODE_TO_FLAT = Map.of(
            "FIXED-01", "fixed_housing",
            "FIXED-02", "fixed_utilities",
            "FIXED-03", "fixed_telecom",
            "FIXED-04", "fixed_insurance",
            "FIXED-05", "fixed_misc",
            "FIXED-06", "fixed_tuition",
            "FIXED-07", "fixed_repayment",
            "FIXED-99", "fixed_misc");

    private static final Map<String, String> KIND_TO_FLAT = Map.of(
            "rent", "fixed_housing",
            "utilities", "fixed_utilities",
            "telecom", "fixed_telecom",
            "insurance", "fixed_insurance",
            "subscription", "fixed_misc",
            "education", "fixed_tuition",
            "repayment", "fixed_repayment",
            "other", "fixed_misc");

    private CategoryFlatFixedTags() {
    }

    public static boolean isFlatFixedTag(String tag) {
        if (StringUtils.isBlank(tag)) {
            return false;
        }
        return tag.trim().startsWith("fixed_") && !"fixed_spending".equals(tag.trim());
    }

    public static String fromCategoryCode(String parentId, String categoryCode) {
        String code = StringUtils.trimToEmpty(categoryCode).toUpperCase(Locale.ROOT);
        if (CODE_TO_FLAT.containsKey(code)) {
            return CODE_TO_FLAT.get(code);
        }
        String parent = StringUtils.trimToEmpty(parentId).toUpperCase(Locale.ROOT);
        if ("FIXED".equals(parent) || code.startsWith("FIXED-")) {
            return "fixed_misc";
        }
        return null;
    }

    public static String fromFixedKind(String fixedKind) {
        if (StringUtils.isBlank(fixedKind)) {
            return "fixed_housing";
        }
        return KIND_TO_FLAT.getOrDefault(fixedKind.trim().toLowerCase(Locale.ROOT), "fixed_misc");
    }

    public static String normalize(String tag, String parentId, String categoryCode) {
        if (isFlatFixedTag(tag)) {
            return tag.trim().toLowerCase(Locale.ROOT);
        }
        if ("fixed_spending".equals(StringUtils.trimToEmpty(tag).toLowerCase(Locale.ROOT))) {
            String fromCode = fromCategoryCode(parentId, categoryCode);
            if (fromCode != null) {
                return fromCode;
            }
            String kind = CategoryFinanceSemantics.inferFixedCostKind(parentId, categoryCode);
            return fromFixedKind(kind);
        }
        return tag;
    }
}
