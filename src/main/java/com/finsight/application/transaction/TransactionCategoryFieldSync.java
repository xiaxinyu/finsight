package com.finsight.application.transaction;

import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;

/**
 * Keeps legacy {@code consume_*} and canonical {@code category_*} columns aligned.
 * {@code consume_code} is the source of truth for classification on {@code transaction}.
 */
public final class TransactionCategoryFieldSync {

    private TransactionCategoryFieldSync() {
    }

    public static String resolveCanonicalCode(Transaction t) {
        if (t == null) {
            return "";
        }
        String code = StringUtils.trimToNull(t.getConsumeCode());
        if (code != null) {
            return code;
        }
        return StringUtils.trimToEmpty(t.getCategoryCode());
    }

    public static void applyCategoryFields(Transaction t, String code, String name) {
        if (t == null) {
            return;
        }
        String canonicalCode = StringUtils.trimToNull(code);
        if (canonicalCode == null) {
            clearCategoryFields(t);
            return;
        }
        String canonicalName = StringUtils.trimToNull(name);
        t.setConsumeCode(canonicalCode);
        t.setCategoryCode(canonicalCode);
        t.setConsumeID(canonicalCode);
        t.setCategoryId(canonicalCode);
        if (canonicalName != null) {
            t.setConsumeName(canonicalName);
            t.setCategoryName(canonicalName);
        }
    }

    public static void clearCategoryFields(Transaction t) {
        if (t == null) {
            return;
        }
        t.setConsumeCode(null);
        t.setCategoryCode(null);
        t.setConsumeID(null);
        t.setCategoryId(null);
        t.setConsumeName(null);
        t.setCategoryName(null);
    }
}
