package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastConfidenceTest {

    @Test
    void forScenario_widensBandForStress() {
        ForecastConfidence.Spread base = ForecastConfidence.forScenario("base");
        ForecastConfidence.Spread stress = ForecastConfidence.forScenario("stress");
        assertTrue(stress.halfWidthPct() > base.halfWidthPct());
    }

    @Test
    void bounds_scaleAroundPointEstimate() {
        ForecastConfidence.Spread spread = ForecastConfidence.forScenario("base");
        assertEquals(900.0, ForecastConfidence.lower(1000, spread));
        assertEquals(1100.0, ForecastConfidence.upper(1000, spread));
    }

    @Test
    void optimistic_hasNarrowerBandThanConservative() {
        ForecastConfidence.Spread optimistic = ForecastConfidence.forScenario("optimistic");
        ForecastConfidence.Spread conservative = ForecastConfidence.forScenario("conservative");
        assertTrue(optimistic.halfWidthPct() < conservative.halfWidthPct());
    }
}
