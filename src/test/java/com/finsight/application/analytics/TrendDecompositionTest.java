package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrendDecompositionTest {

    @Test
    void pctChange_handlesZeroBaseline() {
        assertEquals(100.0, TrendDecomposition.pctChange(0, 500));
        assertEquals(0.0, TrendDecomposition.pctChange(0, 0));
    }

    @Test
    void contributionPct_splitsTotalDelta() {
        assertEquals(60.0, TrendDecomposition.contributionPct(600, 1000));
    }

    @Test
    void lifestyleInflation_detectsExpenseOutpacingIncome() {
        assertTrue(TrendDecomposition.lifestyleInflationDetected(2, 12, 1000));
        assertFalse(TrendDecomposition.lifestyleInflationDetected(10, 12, 1000));
    }

    @Test
    void lifestyleInflation_ignoresSmallExpenseDelta() {
        assertFalse(TrendDecomposition.lifestyleInflationDetected(0, 20, 100));
        assertFalse(TrendDecomposition.lifestyleInflationDetected(2, 12, 200));
    }

    @Test
    void topMovers_ranksByAbsoluteDelta() {
        Map<String, Double> from = Map.of("a", 100.0, "b", 200.0);
        Map<String, Double> to = Map.of("a", 150.0, "b", 500.0);
        Map<String, String> labels = Map.of("a", "Alpha", "b", "Beta");

        List<Map<String, Object>> movers = TrendDecomposition.topMovers(from, to, labels, 350, 5);

        assertEquals("b", movers.get(0).get("key"));
        assertEquals(85.71, ((Number) movers.get(0).get("contributionPct")).doubleValue(), 0.1);
    }
}
