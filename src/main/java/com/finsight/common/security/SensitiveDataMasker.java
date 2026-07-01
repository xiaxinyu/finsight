package com.finsight.common.security;

/**
 * Redact sensitive values for logs and non-privileged API responses.
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String maskCardNumber(String cardNo) {
        if (cardNo == null || cardNo.isBlank()) {
            return "";
        }
        String trimmed = cardNo.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    public static String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "(unset)";
        }
        return "(configured, length=" + secret.trim().length() + ")";
    }
}
