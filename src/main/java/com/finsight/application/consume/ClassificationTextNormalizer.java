package com.finsight.application.consume;

import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands transaction narration into extra phrases for keyword matching (installments, fees, etc.).
 */
public final class ClassificationTextNormalizer {

    private static final Pattern INSTALLMENT_PREFIX =
            Pattern.compile("^[（(]?\\s*分期\\s*[）)]?\\s*");
    private static final Pattern LABEL_BEFORE_DIGITS =
            Pattern.compile("([\\u4e00-\\u9fa5]{2,12})\\d+");
    private static final Pattern CHINESE_PHRASE =
            Pattern.compile("[\\u4e00-\\u9fa5]{2,12}");

    private ClassificationTextNormalizer() {
    }

    /**
     * Returns original text plus derived keywords so {@code contains} rules hit noisy bank descriptions.
     */
    public static String expand(String narration) {
        if (!StringUtils.hasText(narration)) {
            return "";
        }
        String raw = narration.trim();
        Set<String> parts = new LinkedHashSet<>();
        parts.add(raw);

        String noInstallmentPrefix = INSTALLMENT_PREFIX.matcher(raw).replaceFirst("").trim();
        if (StringUtils.hasText(noInstallmentPrefix) && !noInstallmentPrefix.equals(raw)) {
            parts.add(noInstallmentPrefix);
        }

        Matcher labelMatcher = LABEL_BEFORE_DIGITS.matcher(raw);
        while (labelMatcher.find()) {
            parts.add(labelMatcher.group(1));
        }

        Matcher phraseMatcher = CHINESE_PHRASE.matcher(raw);
        while (phraseMatcher.find()) {
            String phrase = phraseMatcher.group();
            if (!isNoisePhrase(phrase)) {
                parts.add(phrase);
            }
        }

        if (raw.contains("邮购分期")) {
            parts.add("邮购分期");
        }
        if (raw.contains("免年费")) {
            parts.add("免年费");
        }
        if (raw.contains("分期")) {
            parts.add("分期");
        }

        return String.join(" ", parts);
    }

    private static boolean isNoisePhrase(String phrase) {
        if (!StringUtils.hasText(phrase) || phrase.length() < 2) {
            return true;
        }
        return "分期".equals(phrase) || "消费".equals(phrase) || "交易".equals(phrase);
    }
}
