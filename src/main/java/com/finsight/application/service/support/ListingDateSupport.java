package com.finsight.application.service.support;

import com.finsight.core.AppServiceException;
import com.finsight.core.DateParseException;
import com.finsight.core.DateTool;
import com.finsight.core.StringTool;

import java.text.SimpleDateFormat;
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

    /** Parses UI date string (MM/dd/yyyy) for {@link com.finsight.domain.model.Transaction} and similar queries. */
    public static Date parseMmDdYyyy(String mmddyyyy) throws AppServiceException {
        try {
            return DateTool.changeStringToDate(mmddyyyy, DateTool.DF_MM_DD_YYYY);
        } catch (DateParseException e) {
            throw new AppServiceException("Invalid date format", e);
        }
    }

    private static String toYearMonth(String mmddyyyy) throws AppServiceException {
        Date d = parseMmDdYyyy(mmddyyyy);
        return new SimpleDateFormat("yyyy-MM").format(d);
    }
}
