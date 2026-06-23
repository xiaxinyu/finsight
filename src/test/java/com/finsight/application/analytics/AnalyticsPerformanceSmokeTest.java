package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lightweight performance/cache smoke tests for v2.0.0 analytics read paths.
 */
class AnalyticsPerformanceSmokeTest {

    @Test
    void resultCache_expiresAfterTtl() throws InterruptedException {
        AnalyticsResultCache<String> cache = new AnalyticsResultCache<>(1_000);
        cache.put("k", "v");
        assertEquals("v", cache.get("k"));
        Thread.sleep(1_100);
        assertNull(cache.get("k"));
    }

    @Test
    void forecastScenarioParams_cacheKeyIsStable() {
        ForecastScenarioParams empty = ForecastScenarioParams.empty();
        ForecastScenarioParams adjusted = ForecastScenarioParams.fromMap(Map.of("incomeChangePct", -5));
        assertNotNull(empty.cacheKey());
        assertTrue(adjusted.cacheKey().contains("-5"));
    }

    @Test
    void combinedInsightContext_topCardsRespectsLimit() {
        CombinedInsightContext ctx = new CombinedInsightContext(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(
                        Map.of("id", "a", "priority", 10),
                        Map.of("id", "b", "priority", 90),
                        Map.of("id", "c", "priority", 50)
                ));
        assertEquals(2, ctx.topCards(2).size());
        assertEquals("b", ctx.topCards(2).get(0).get("id"));
    }
}
