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

    /** Inclusive last day counted for consumption in a calendar year (YTD when year is current). */
    public static LocalDate consumptionYearEndInclusive(int year, LocalDate asOf) {
        if (year < asOf.getYear()) {
            return LocalDate.of(year, 12, 31);
        }
        if (year > asOf.getYear()) {
            return LocalDate.of(year, 1, 1).minusDays(1);
        }
        return asOf;
    }

    /** Same month/day in the prior year for fair YTD YoY (handles Feb 29). */
    public static LocalDate alignedPriorYearDay(int currentYear, LocalDate asOf) {
        int priorYear = currentYear - 1;
        int day = Math.min(asOf.getDayOfMonth(), LocalDate.of(priorYear, asOf.getMonth(), 1).lengthOfMonth());
        return LocalDate.of(priorYear, asOf.getMonth(), day);
    }

    public static HalfOpen consumptionYearRange(int year, LocalDate asOf) {
        LocalDate endInc = consumptionYearEndInclusive(year, asOf);
        return new HalfOpen(LocalDate.of(year, 1, 1), endInc.plusDays(1));
    }

    /**
     * Range for YoY compare year: when comparing to the current calendar year,
     * prior year uses the same day-of-year window (not full prior year).
     */
    public static HalfOpen yoyCompareYearRange(int year, int toYear, LocalDate asOf) {
        if (year == toYear - 1 && toYear == asOf.getYear()) {
            LocalDate endInc = alignedPriorYearDay(toYear, asOf);
            return new HalfOpen(LocalDate.of(year, 1, 1), endInc.plusDays(1));
        }
        return consumptionYearRange(year, asOf);
    }

    public static boolean isPartialConsumptionYear(int year, LocalDate asOf) {
        return year == asOf.getYear() && asOf.getDayOfYear() < LocalDate.of(year, 12, 31).getDayOfYear();
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
