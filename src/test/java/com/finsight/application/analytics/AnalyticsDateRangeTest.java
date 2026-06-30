package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsDateRangeTest {

    @Test
    void toLocalDate_convertsUtilDateWithoutLocaleStringParsing() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        assertEquals(LocalDate.of(2026, 1, 1), AnalyticsDateRange.toLocalDate(cal.getTime()));
    }

    @Test
    void toLocalDate_parsesIsoString() {
        assertEquals(LocalDate.of(2026, 6, 30), AnalyticsDateRange.toLocalDate("2026-06-30"));
    }
}
