package com.finsight.application.analytics;

import java.time.LocalDate;
import java.util.Date;

/**
 * Half-open {@code [start, end)} date ranges for sargable analytics SQL.
 */
public final class AnalyticsDateRange {

    public record HalfOpen(LocalDate startInclusive, LocalDate endExclusive) {
    }

    private AnalyticsDateRange() {
    }

    public static HalfOpen calendarYear(int year) {
        return new HalfOpen(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1));
    }

    public static LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (value instanceof Date d) {
            return new java.sql.Date(d.getTime()).toLocalDate();
        }
        String text = String.valueOf(value);
        return LocalDate.parse(text.length() >= 10 ? text.substring(0, 10) : text);
    }
}
