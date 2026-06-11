package com.finsight.application.consume;

import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;

/**
 * Builds a single text blob for rule matching from multiple transaction fields.
 */
public final class ClassificationNarrationBuilder {

    private ClassificationNarrationBuilder() {
    }

    public static String fromTransaction(Transaction t) {
        if (t == null) {
            return "";
        }
        return join(
                t.getTransactionDesc(),
                t.getOpponentName(),
                t.getDemoArea());
    }

    public static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String p = StringUtils.trimToEmpty(part);
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(p);
        }
        return sb.toString();
    }
}
