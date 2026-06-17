package com.finsight.application.query;

import com.finsight.web.api.dto.TransactionParam;

import java.util.Locale;
import java.util.Set;

/**
 * Whitelisted server-side sort for transaction listing queries.
 */
public final class TransactionSort {

    public static final String FIELD_DATE = "transactionDate";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_CARD = "card";
    public static final String FIELD_TYPE = "type";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            FIELD_DATE, FIELD_AMOUNT, FIELD_CARD, FIELD_TYPE);

    private TransactionSort() {
    }

    public static void apply(TransactionParam param, TransactionQuery query) {
        if (param == null || query == null) {
            return;
        }
        String field = normalizeField(param.getSortField());
        if (field == null) {
            return;
        }
        query.setSortField(field);
        query.setSortOrder(normalizeOrder(param.getSortOrder()));
    }

    static String normalizeField(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return ALLOWED_FIELDS.contains(trimmed) ? trimmed : null;
    }

    static String normalizeOrder(String raw) {
        if (raw == null) {
            return "desc";
        }
        return "asc".equals(raw.trim().toLowerCase(Locale.ROOT)) ? "asc" : "desc";
    }
}
