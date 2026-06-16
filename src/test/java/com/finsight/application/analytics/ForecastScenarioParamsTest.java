package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastScenarioParamsTest {

    @Test
    void fromMap_parsesKnownFields() {
        ForecastScenarioParams params = ForecastScenarioParams.fromMap(Map.of(
                "incomeChangePct", -8,
                "newMonthlyBill", 500,
                "lumpSumExpense", 12000,
                "targetMonthlyPayment", 8000
        ));

        assertEquals(-8.0, params.incomeChangePct());
        assertEquals(500.0, params.newMonthlyBill());
        assertEquals(12000.0, params.lumpSumExpense());
        assertEquals(8000.0, params.targetMonthlyPayment());
        assertTrue(params.hasAdjustments());
    }

    @Test
    void explanationLines_describeAppliedAdjustments() {
        ForecastScenarioParams params = new ForecastScenarioParams(-10.0, 300.0, 5000.0, null);
        assertEquals(3, params.explanationLines().size());
        assertTrue(params.explanationLines().get(0).contains("-10.0%"));
    }

    @Test
    void empty_hasNoAdjustments() {
        ForecastScenarioParams params = ForecastScenarioParams.empty();
        assertFalse(params.hasAdjustments());
        assertTrue(params.explanationLines().isEmpty());
    }
}
