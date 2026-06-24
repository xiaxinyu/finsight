package com.finsight.application.analytics;

import com.finsight.domain.model.Bill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastProjectionTest {

    @Test
    void assessQuality_requiresTwelveMonthsForSeasonal() {
        assertFalse(ForecastProjection.assessQuality(6).seasonalAllowed());
        assertTrue(ForecastProjection.assessQuality(12).seasonalAllowed());
        assertEquals("low", ForecastProjection.assessQuality(2).confidenceLevel());
    }

    @Test
    void projectMonth_usesPriorValuesOnly() {
        List<ForecastProjection.YearMonthValue> series = List.of(
                new ForecastProjection.YearMonthValue(YearMonth.of(2025, 1), 1000),
                new ForecastProjection.YearMonthValue(YearMonth.of(2025, 2), 1100),
                new ForecastProjection.YearMonthValue(YearMonth.of(2025, 3), 1200));
        List<Double> prior = ForecastProjection.priorValues(series, YearMonth.of(2025, 3));
        double projected = ForecastProjection.projectMonth(prior, 3, series, 1.0);
        assertTrue(projected > 0);
        assertEquals(2, prior.size());
    }

    @Test
    void toSeries_sortsChronologically() {
        Map<String, Double> raw = Map.of("2025-03", 3.0, "2025-01", 1.0, "2025-02", 2.0);
        List<ForecastProjection.YearMonthValue> series = ForecastProjection.toSeries(raw);
        assertEquals(YearMonth.of(2025, 1), series.get(0).ym());
        assertEquals(YearMonth.of(2025, 3), series.get(2).ym());
    }
}
