package com.finsight.application.transaction;

import com.finsight.domain.model.Transaction;

/**
 * Truncates string fields to match {@code transaction} / {@code transaction_temp} column limits.
 */
public final class TransactionFieldSanitizer {

    public static final int OPPONENT_NAME_MAX = 128;
    public static final int OPPONENT_ACCOUNT_MAX = 64;

    private TransactionFieldSanitizer() {
    }

    public static void sanitize(Transaction t) {
        if (t == null) {
            return;
        }
        t.setOpponentName(truncate(t.getOpponentName(), OPPONENT_NAME_MAX));
        t.setOpponentAccount(truncate(t.getOpponentAccount(), OPPONENT_ACCOUNT_MAX));
    }

    static String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
