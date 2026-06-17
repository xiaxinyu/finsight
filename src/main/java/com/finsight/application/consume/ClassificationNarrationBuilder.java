package com.finsight.application.consume;

import com.finsight.domain.model.Transaction;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds a single text blob for rule matching from multiple transaction fields.
 */
public final class ClassificationNarrationBuilder {

    private static final Pattern PAYMENT_CHANNEL = Pattern.compile(
            "(?:alipay|wechat\\s*pay|wxpay|tenpay|unionpay|银联|支付宝|微信支付|财付通|快捷支付|云闪付)",
            Pattern.CASE_INSENSITIVE);

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

    /**
     * Merchant-focused narration for import classification — strips payment-channel prefixes
     * common on CCB/Tenpay credit-card exports (e.g. {@code 财付通-深圳市地铁…}).
     */
    public static String forMatching(Transaction t) {
        return merchantCore(fromTransaction(t));
    }

    public static String merchantCore(String raw) {
        String trimmed = StringUtils.trimToEmpty(raw);
        if (trimmed.isEmpty()) {
            return "";
        }
        String merchant = stripPaymentChannelSegments(trimmed);
        return StringUtils.isNotBlank(merchant) ? merchant : trimmed;
    }

    static String stripPaymentChannelSegments(String raw) {
        String[] parts = raw.split("[-－—|/：:]");
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            String p = StringUtils.trimToEmpty(part);
            if (p.isEmpty()) {
                continue;
            }
            if (isPaymentChannelSegment(p)) {
                continue;
            }
            kept.add(p);
        }
        if (kept.isEmpty()) {
            return PAYMENT_CHANNEL.matcher(raw).replaceAll("").replaceAll("\\s+", " ").trim();
        }
        return String.join(" ", kept);
    }

    private static boolean isPaymentChannelSegment(String segment) {
        String s = segment.trim();
        if (s.isEmpty()) {
            return true;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        if (PAYMENT_CHANNEL.matcher(lower).find()) {
            return true;
        }
        return "消费".equals(s) || "支付".equals(s) || "快捷".equals(s);
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
