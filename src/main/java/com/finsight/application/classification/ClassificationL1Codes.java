package com.finsight.application.classification;

import java.util.Set;

/**
 * Known L1 {@code cls_category.code} values used in reports and seed planning.
 */
public final class ClassificationL1Codes {

    /** v1.8 seed income L1 — superseded by {@link #INC} after dedup. */
    public static final String INCOME = "INCOME";
    /** Canonical income L1 after dedup (preferred when present). */
    public static final String INC = "INC";
    public static final String FIXED = "FIXED";
    public static final String LIVING = "LIVING";
    public static final String SHOPPING = "SHOPPING";
    /** v1.8 seed transport L1 — superseded by {@link #TRANSPORT} on some installs. */
    public static final String TRAVEL = "TRAVEL";
    /** Legacy/canonical transport L1 after dedup (preferred when present). */
    public static final String TRANSPORT = "TRANSPORT";
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
            INCOME, INC, FIXED, LIVING, SHOPPING, TRAVEL, TRANSPORT, EDU, ENT, GIFT, REIM,
            ASSET, LIABILITY, INVEST, WEALTH, FEE, OTHER);

    private ClassificationL1Codes() {
    }

    public static Set<String> all() {
        return ALL;
    }

    public static boolean isKnownL1(String code) {
        return code != null && ALL.contains(code.trim());
    }

    /**
     * Pick the income L1 root that exists in the database (post-dedup prefers {@code INC}).
     */
    public static String resolveIncomeL1(Set<String> existingCodes) {
        Set<String> existing = existingCodes == null ? Set.of() : existingCodes;
        if (existing.contains(INC)) {
            return INC;
        }
        if (existing.contains(INCOME)) {
            return INCOME;
        }
        return INC;
    }

    /**
     * Pick the transport L1 root that exists (post-dedup prefers {@code TRANSPORT}).
     */
    public static String resolveTransportL1(Set<String> existingCodes) {
        Set<String> existing = existingCodes == null ? Set.of() : existingCodes;
        if (existing.contains(TRANSPORT)) {
            return TRANSPORT;
        }
        if (existing.contains(TRAVEL)) {
            return TRAVEL;
        }
        return TRANSPORT;
    }

    /** Resolve catalog parent to the L1 code that should appear in {@code parent_id}. */
    public static String resolveParentL1(String catalogParent, Set<String> existingCodes) {
        if (catalogParent == null) {
            return null;
        }
        String parent = catalogParent.trim();
        if (INC.equals(parent) || INCOME.equals(parent)) {
            return resolveIncomeL1(existingCodes);
        }
        if (TRAVEL.equals(parent) || TRANSPORT.equals(parent)) {
            return resolveTransportL1(existingCodes);
        }
        return parent;
    }
}
