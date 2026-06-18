package com.finsight.application.analytics;

/**
 * Shared merchant-token contract between Java {@link MerchantNormalizer} and MySQL
 * {@code finsight_normalize_merchant_token()}. Drilldown SQL and {@code v_transaction_analytics}
 * must use the same normalization as merchant mining / trend reports.
 */
public final class MerchantTokenSql {

    /** MySQL function installed by {@code V22__merchant_token_normalization.sql}. */
    public static final String NORMALIZE_FUNCTION = "finsight_normalize_merchant_token";

    /** Raw merchant text from transaction opponent_name / transaction_desc (MyBatis alias {@code t}). */
    public static final String MERCHANT_RAW_T =
            "coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), '')";

    private MerchantTokenSql() {
    }

    public static String normalizeCall(String rawExpression) {
        return NORMALIZE_FUNCTION + "(" + rawExpression + ")";
    }

    public static String normalizedTokenT() {
        return normalizeCall(MERCHANT_RAW_T);
    }
}
