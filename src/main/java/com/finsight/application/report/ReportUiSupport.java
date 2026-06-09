package com.finsight.application.report;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Server-side helpers mirroring chart/table UI rules for reports.
 */
public final class ReportUiSupport {

    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern SLASH_DATE = Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}$");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter SLASH = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);

    private ReportUiSupport() {
    }

    public static String formatAxisDateLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        LocalDate date = tryParseDate(trimmed);
        if (date != null) {
            return String.format(Locale.US, "%02d/%02d", date.getMonthValue(), date.getDayOfMonth());
        }
        if (trimmed.length() >= 5 && trimmed.charAt(2) == '/') {
            return trimmed.substring(0, 5);
        }
        return trimmed;
    }

    public static boolean shouldRotateAxisLabels(List<String> categories) {
        return daySpan(categories) > 28;
    }

    public static int axisLabelRotation(List<String> categories) {
        return shouldRotateAxisLabels(categories) ? 35 : 0;
    }

    public static int axisLabelInterval(int categoryCount) {
        if (categoryCount <= 0) {
            return 0;
        }
        int targetTicks = Math.min(12, Math.max(6, categoryCount <= 12 ? categoryCount : 10));
        if (categoryCount <= targetTicks) {
            return 0;
        }
        return (int) Math.ceil((double) categoryCount / targetTicks) - 1;
    }

    public static boolean hidePointMarkers(int seriesLength) {
        return seriesLength > 60;
    }

    public static long daySpan(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return 0;
        }
        LocalDate min = null;
        LocalDate max = null;
        for (String category : categories) {
            LocalDate date = tryParseDate(category);
            if (date == null) {
                continue;
            }
            if (min == null || date.isBefore(min)) {
                min = date;
            }
            if (max == null || date.isAfter(max)) {
                max = date;
            }
        }
        if (min == null || max == null) {
            return categories.size();
        }
        return ChronoUnit.DAYS.between(min, max) + 1;
    }

    public static String deltaCssClass(double deltaPercent) {
        if (deltaPercent < 0) {
            return "fs-delta-negative";
        }
        if (deltaPercent > 0) {
            return "fs-delta-positive";
        }
        return "fs-delta-neutral";
    }

    public static String formatNumberOnly(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private static LocalDate tryParseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            if (ISO_DATE.matcher(trimmed).matches()) {
                return LocalDate.parse(trimmed, ISO);
            }
            if (SLASH_DATE.matcher(trimmed).matches()) {
                return LocalDate.parse(trimmed, SLASH);
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
    }
}
