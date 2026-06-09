package com.finsight.application.report;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportUiSupportTest {

    @Test
    void formatAxisDateLabel_formatsIsoDatesAsMmDd() {
        assertEquals("04/17", ReportUiSupport.formatAxisDateLabel("2026-04-17"));
        assertEquals("01/05", ReportUiSupport.formatAxisDateLabel("2026-01-05"));
    }

    @Test
    void formatAxisDateLabel_preservesShortSlashLabels() {
        assertEquals("04/17", ReportUiSupport.formatAxisDateLabel("04/17/2026"));
    }

    @Test
    void shouldRotateAxisLabels_whenSpanExceeds28Days() {
        List<String> longSpan = Arrays.asList("2026-01-01", "2026-02-15");
        assertTrue(ReportUiSupport.shouldRotateAxisLabels(longSpan));
        assertEquals(35, ReportUiSupport.axisLabelRotation(longSpan));
    }

    @Test
    void shouldNotRotateAxisLabels_forShortSpan() {
        List<String> shortSpan = Arrays.asList("2026-04-01", "2026-04-20");
        assertFalse(ReportUiSupport.shouldRotateAxisLabels(shortSpan));
        assertEquals(0, ReportUiSupport.axisLabelRotation(shortSpan));
    }

    @Test
    void axisLabelInterval_targetsReadableTickDensity() {
        assertEquals(0, ReportUiSupport.axisLabelInterval(8));
        assertTrue(ReportUiSupport.axisLabelInterval(48) >= 3);
    }

    @Test
    void hidePointMarkers_forLongSeries() {
        assertFalse(ReportUiSupport.hidePointMarkers(30));
        assertTrue(ReportUiSupport.hidePointMarkers(61));
    }

    @Test
    void daySpan_countsInclusiveRange() {
        List<String> categories = Arrays.asList("2026-04-01", "2026-04-10");
        assertEquals(10, ReportUiSupport.daySpan(categories));
        assertEquals(0, ReportUiSupport.daySpan(Collections.emptyList()));
    }

    @Test
    void deltaCssClass_usesSemanticColors() {
        assertEquals("fs-delta-negative", ReportUiSupport.deltaCssClass(-1.2));
        assertEquals("fs-delta-positive", ReportUiSupport.deltaCssClass(0.5));
        assertEquals("fs-delta-neutral", ReportUiSupport.deltaCssClass(0));
    }

    @Test
    void formatNumberOnly_formatsWithoutCurrencySymbol() {
        assertEquals("1,234.50", ReportUiSupport.formatNumberOnly(1234.5));
    }

    @Test
    void isSeriesEmpty_detectsZeroOrMissingValues() {
        assertTrue(ReportUiSupport.isSeriesEmpty(Collections.emptyList()));
        assertTrue(ReportUiSupport.isSeriesEmpty(Arrays.asList(0, 0.0, null)));
        assertFalse(ReportUiSupport.isSeriesEmpty(Arrays.asList(0, 42.5)));
    }
}
