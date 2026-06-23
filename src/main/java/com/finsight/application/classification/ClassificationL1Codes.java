package com.finsight.application.classification;

import java.util.Set;

/**
 * Known L1 {@code cls_category.code} values used in reports and seed planning.
 */
public final class ClassificationL1Codes {

    public static final String INCOME = "INCOME";
    public static final String FIXED = "FIXED";
    public static final String LIVING = "LIVING";
    public static final String SHOPPING = "SHOPPING";
    public static final String TRAVEL = "TRAVEL";
    public static final String EDU = "EDU";
    public static final String ENT = "ENT";
    public static final String GIFT = "GIFT";
    public static final String REIM = "REIM";
    public static final String ASSET = "ASSET";
    public static final String LIABILITY = "LIABILITY";
    public static final String INVEST = "INVEST";
    public static final String WEALTH = "WEALTH";
    public static final String FEE = "FEE";
    public static final String OTHER = "OTHER";

    private static final Set<String> ALL = Set.of(
            INCOME, FIXED, LIVING, SHOPPING, TRAVEL, EDU, ENT, GIFT, REIM,
            ASSET, LIABILITY, INVEST, WEALTH, FEE, OTHER);

    private ClassificationL1Codes() {
    }

    public static Set<String> all() {
        return ALL;
    }

    public static boolean isKnownL1(String code) {
        return code != null && ALL.contains(code.trim());
    }
}
