package com.finsight.application.analytics;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Strips payment noise from raw bank descriptions and produces stable merchant tokens.
 */
public final class MerchantNormalizer {

    private static final Pattern ORDER_NO = Pattern.compile(
            "(?:订单|order\\s*no\\.?\\s*:?|ord(?:er)?\\s*[#:])\\s*\\d{4,}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STORE_NO = Pattern.compile(
            "(?:门店|store|branch|shop)\\s*[#:]?\\s*\\d{2,}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_DIGITS = Pattern.compile("\\s+\\d{4,}$");
    private static final Pattern PAYMENT_CHANNEL = Pattern.compile(
            "(?:alipay|wechat\\s*pay|wxpay|tenpay|unionpay|银联|支付宝|微信支付|财付通)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN_SUFFIX = Pattern.compile("\\.(com|cn|net|io)$");
    private static final Pattern TRAILING_NOISE = Pattern.compile(
            "\\s+(trip|trips|ride|rides|monthly|annual|subscription|mktp)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private MerchantNormalizer() {
    }

    public static String rawMerchant(String opponentName, String transactionDesc) {
        String opponent = opponentName == null ? "" : opponentName.trim();
        if (!opponent.isEmpty()) {
            return opponent;
        }
        return transactionDesc == null ? "" : transactionDesc.trim();
    }

    public static String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.toLowerCase(Locale.ROOT).trim();
        normalized = ORDER_NO.matcher(normalized).replaceAll("");
        normalized = STORE_NO.matcher(normalized).replaceAll("");
        normalized = PAYMENT_CHANNEL.matcher(normalized).replaceAll("");
        normalized = TRAILING_DIGITS.matcher(normalized).replaceAll("");
        normalized = TRAILING_NOISE.matcher(normalized).replaceAll("");
        normalized = DOMAIN_SUFFIX.matcher(normalized).replaceAll("");
        normalized = MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();
        return normalized;
    }

    public static String displayName(String normalizedToken, String preferredRaw) {
        if (preferredRaw != null && !preferredRaw.isBlank()) {
            String cleaned = preferredRaw.trim();
            if (cleaned.length() <= 48) {
                return cleaned;
            }
            return cleaned.substring(0, 45) + "...";
        }
        if (normalizedToken == null || normalizedToken.isBlank()) {
            return "";
        }
        String[] parts = normalizedToken.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
