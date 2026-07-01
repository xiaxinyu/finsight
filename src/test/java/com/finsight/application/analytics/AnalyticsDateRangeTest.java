package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsDateRangeTest {

    @Test
    void consumptionYearEndInclusive_capsCurrentYearAtAsOf() {
        LocalDate asOf = LocalDate.of(2026, 6, 26);
        assertEquals(LocalDate.of(2025, 12, 31), AnalyticsDateRange.consumptionYearEndInclusive(2025, asOf));
        assertEquals(asOf, AnalyticsDateRange.consumptionYearEndInclusive(2026, asOf));
    }

    @Test
    void alignedPriorYearDay_handlesLeapYear() {
        LocalDate asOf = LocalDate.of(2024, 2, 29);
        assertEquals(LocalDate.of(2023, 2, 28), AnalyticsDateRange.alignedPriorYearDay(2024, asOf));
    }

    @Test
    void yoyCompareYearRange_alignsPriorYearWhenCurrentYear() {
        LocalDate asOf = LocalDate.of(2026, 6, 26);
        var range = AnalyticsDateRange.yoyCompareYearRange(2025, 2026, asOf);
        assertEquals(LocalDate.of(2025, 1, 1), range.startInclusive());
        assertEquals(LocalDate.of(2025, 6, 27), range.endExclusive());
    }

    @Test
    void isPartialConsumptionYear() {
        LocalDate asOf = LocalDate.of(2026, 6, 26);
        assertFalse(AnalyticsDateRange.isPartialConsumptionYear(2025, asOf));
        assertTrue(AnalyticsDateRange.isPartialConsumptionYear(2026, asOf));
    }
}
