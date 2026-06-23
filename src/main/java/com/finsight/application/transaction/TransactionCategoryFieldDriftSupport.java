package com.finsight.application.transaction;

import org.apache.commons.lang3.StringUtils;

/**
 * Detects drift between {@code transaction} category columns and {@code cls_category}.
 */
public final class TransactionCategoryFieldDriftSupport {

    private TransactionCategoryFieldDriftSupport() {
    }

    public static boolean isDrift(String consumeCode,
                                 String consumeId,
                                 String consumeName,
                                 String categoryCode,
                                 String categoryName,
                                 String categoryId,
                                 String catalogCode,
                                 String catalogName,
                                 String catalogId) {
        String code = StringUtils.trimToNull(consumeCode);
        if (code == null) {
            return false;
        }
        String canonicalCode = StringUtils.trimToNull(catalogCode);
        if (canonicalCode == null) {
            return false;
        }
        if (!code.equals(canonicalCode)) {
            return true;
        }
        if (!StringUtils.equals(StringUtils.trimToEmpty(consumeName), StringUtils.trimToEmpty(catalogName))) {
            return true;
        }
        if (!isAllowedIdRef(consumeId, code, catalogId)) {
            return true;
        }
        String catCode = StringUtils.trimToNull(categoryCode);
        if (catCode != null && !code.equals(catCode)) {
            return true;
        }
        if (StringUtils.isNotBlank(categoryName)
                && !StringUtils.equals(categoryName.trim(), StringUtils.trimToEmpty(catalogName))) {
            return true;
        }
        if (!isAllowedIdRef(categoryId, code, catalogId)) {
            return true;
        }
        return false;
    }

    static boolean isAllowedIdRef(String ref, String code, String catalogId) {
        String trimmed = StringUtils.trimToEmpty(ref);
        if (trimmed.isEmpty()) {
            return true;
        }
        return trimmed.equals(code) || trimmed.equals(StringUtils.trimToEmpty(catalogId));
    }
}
