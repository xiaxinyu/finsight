package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedInsightBuilderTest {

    @Test
    void build_returnsAtLeastThreeCombinedKindsWhenSignalsPresent() {
        Map<String, Object> profile = profile("high_fixed_burden", 35);
        Map<String, Object> trends = trendsWithMovers();
        Map<String, Object> forecast = Map.of(
                "deficitMonths", List.of("2026-03", "2026-04"),
                "months", List.of(Map.of("yearMonth", "2026-03", "net", -1200)),
                "budgetSuggestion", Map.of("monthlyCap", 8000));
        Map<String, Object> subscriptions = Map.of(
                "subscriptions", List.of(Map.of(
                        "merchantToken", "netflix",
                        "displayName", "Netflix",
                        "cadence", "monthly",
                        "monthlyEquivalent", 65,
                        "avgAmount", 65)),
                "summary", Map.of("count", 1, "monthlyTotal", 65));
        Map<String, Object> concentration = Map.of("top3SharePct", 42.0, "merchants", List.of());

        List<Map<String, Object>> cards = CombinedInsightBuilder.build(
                profile, trends, forecast, subscriptions, concentration);

        assertTrue(cards.size() >= 3);
        assertTrue(cards.stream().anyMatch(c -> "combined_archetype_trend".equals(c.get("id"))));
        assertTrue(cards.stream().anyMatch(c -> "combined_forecast_pressure".equals(c.get("id"))));
        assertTrue(cards.stream().anyMatch(c -> "combined_subscription_review".equals(c.get("id"))));
        Map<String, Object> trendCard = cards.stream()
                .filter(c -> "combined_archetype_trend".equals(c.get("id")))
                .findFirst()
                .orElseThrow();
        assertEquals("combined", trendCard.get("type"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) trendCard.get("sections");
        assertFalse(sections.isEmpty());
        assertTrue(String.valueOf(trendCard.get("reason")).contains("High fixed burden"));
    }

    @Test
    void build_includesDataQualityCardWhenTrustIsLow() {
        Map<String, Object> profile = profile("data_quality_risk", 30);
        List<Map<String, Object>> cards = CombinedInsightBuilder.build(
                profile, Map.of(), Map.of(), Map.of(), Map.of());

        assertTrue(cards.stream().anyMatch(c -> "combined_data_quality".equals(c.get("id"))));
    }

    private static Map<String, Object> profile(String userType, double dataTrustScore) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userType", userType);
        profile.put("userTypeExplanation", "Test archetype explanation");
        profile.put("dimensions", List.of(Map.of(
                "id", "data_trust",
                "score", dataTrustScore,
                "summary", "classify rows",
                "reason", "12 unclassified transactions",
                "evidence", List.of(Map.of(
                        "source", "quality",
                        "ref", "unclassifiedCount",
                        "label", "Unclassified",
                        "detail", "Needs review",
                        "value", "12 unclassified")))));
        return profile;
    }

    private static Map<String, Object> trendsWithMovers() {
        Map<String, Object> trends = new LinkedHashMap<>();
        trends.put("fromYear", 2025);
        trends.put("toYear", 2026);
        trends.put("summary", Map.of(
                "expense", Map.of("deltaAmount", 2500, "deltaPercent", 12)));
        trends.put("topCategoryGrowth", List.of(Map.of(
                "categoryCode", "FOOD",
                "categoryName", "Food",
                "deltaAmount", 1200,
                "contributionPct", 48)));
        trends.put("topMerchantMovers", List.of(Map.of(
                "merchantToken", "rent",
                "label", "Landlord",
                "deltaAmount", 800,
                "contributionPct", 32)));
        return trends;
    }
}
