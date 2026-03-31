package com.finsight.application.support;

import com.finsight.core.AppServiceException;
import com.finsight.core.DateParseException;
import com.finsight.core.DateTool;
import com.finsight.core.StringTool;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Shared parsing for EasyUI date fields ({@link DateTool#DF_MM_DD_YYYY}): transaction lists, salary/rent filters,
 * and {@code yyyy-MM} month bounds for insurance/benefit {@code time} columns.
 */
public final class ListingDateSupport {

    private ListingDateSupport() {
    }

    /**
     * Maps UI date range to {@code yyyy-MM} inclusive bounds for filtering {@code time}-style columns.
     *
     * @return {@code [timeFrom, timeTo]} — each element may be null if the corresponding input was blank
     */
    public static String[] monthRangeOrNull(String transactionDateStartStr, String transactionDateEndStr)
            throws AppServiceException {
        String from = null;
        String to = null;
        if (!StringTool.isNullOrEmpty(transactionDateStartStr)) {
            from = toYearMonth(transactionDateStartStr);
        }
        if (!StringTool.isNullOrEmpty(transactionDateEndStr)) {
            to = toYearMonth(transactionDateEndStr);
        }
        return new String[]{from, to};
    }

    /**
     * Same as {@link #monthRangeOrNull(String, String)} but defaults to current calendar year
     * ({@code yyyy-01} ~ {@code yyyy-12}) when both inputs are blank.
     */
    public static String[] monthRangeOrDefaultOneYear(String transactionDateStartStr, String transactionDateEndStr)
            throws AppServiceException {
        String[] ym = monthRangeOrNull(transactionDateStartStr, transactionDateEndStr);
        if (ym[0] != null || ym[1] != null) {
            return ym;
        }
        Calendar cal = Calendar.getInstance();
        String year = new SimpleDateFormat("yyyy").format(cal.getTime());
        String from = year + "-01";
        String to = year + "-12";
        return new String[]{from, to};
    }

    /** Parses UI date string (MM/dd/yyyy) for {@link com.finsight.domain.model.Transaction} and similar queries. */
    public static Date parseMmDdYyyy(String mmddyyyy) throws AppServiceException {
        try {
            return DateTool.changeStringToDate(mmddyyyy, DateTool.DF_MM_DD_YYYY);
        } catch (DateParseException e) {
            throw new AppServiceException("Invalid date format", e);
        }
    }

    /**
     * Parses date strings if present; when both are blank returns current calendar year range
     * [yyyy-01-01 00:00:00, yyyy-12-31 23:59:59 (date granularity)].
     */
    public static Date[] parseMmDdYyyyOrDefaultOneYear(String transactionDateStartStr, String transactionDateEndStr)
            throws AppServiceException {
        Date from = null;
        Date to = null;
        if (!StringTool.isNullOrEmpty(transactionDateStartStr)) {
            from = parseMmDdYyyy(transactionDateStartStr);
        }
        if (!StringTool.isNullOrEmpty(transactionDateEndStr)) {
            to = parseMmDdYyyy(transactionDateEndStr);
        }
        if (from != null || to != null) {
            return new Date[]{from, to};
        }
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDate = cal.getTime();
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date endDate = cal.getTime();
        return new Date[]{startDate, endDate};
    }

    private static String toYearMonth(String mmddyyyy) throws AppServiceException {
        Date d = parseMmDdYyyy(mmddyyyy);
        return new SimpleDateFormat("yyyy-MM").format(d);
    }
}
