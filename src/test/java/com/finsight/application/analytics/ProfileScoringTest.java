package com.finsight.application.analytics;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileScoringTest {

    @Test
    void scoreSpendingConcentration_penalizesHighTopShare() {
        assertTrue(ProfileScoring.scoreSpendingConcentration(25) > ProfileScoring.scoreSpendingConcentration(55));
        assertEquals(40.0, ProfileScoring.scoreSpendingConcentration(50));
    }

    @Test
    void scoreIncomeStability_rewardsSteadyIncome() {
        List<Double> steady = List.of(10000.0, 10100.0, 9900.0, 10050.0);
        List<Double> swingy = List.of(5000.0, 15000.0, 7000.0, 13000.0);
        assertTrue(ProfileScoring.scoreIncomeStability(steady) > ProfileScoring.scoreIncomeStability(swingy));
    }

    @Test
    void weightedOverallScore_usesWeightTable() {
        Map<String, Double> scores = scores(
                "data_trust", 80,
                "income_stability", 70,
                "spending_control", 70,
                "savings_discipline", 70,
                "fixed_burden", 70,
                "liquidity_safety", 70,
                "debt_pressure", 70,
                "lifestyle_inflation", 70,
                "spending_concentration", 70,
                "seasonality_risk", 70);
        double weighted = ProfileScoring.weightedOverallScore(scores);
        assertEquals(71.5, weighted);
        int sum = ProfileScoring.DIMENSION_WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(100, sum);
    }

    @Test
    void overallConfidence_degradesWithLowDataTrust() {
        assertEquals("low", ProfileScoring.overallConfidence(40, 12, false));
        assertEquals("medium", ProfileScoring.overallConfidence(60, 6, false));
        assertEquals("high", ProfileScoring.overallConfidence(80, 12, false));
        assertEquals("low", ProfileScoring.overallConfidence(80, 12, true));
    }

    @Test
    void classifyUserType_prioritizesDataQualityRisk() {
        ProfileScoring.UserTypeResult result = ProfileScoring.classifyUserType(scores(
                "data_trust", 30,
                "savings_discipline", 90));
        assertEquals("data_quality_risk", result.type());
        assertTrue(result.explanation().contains("unclassified"));
    }

    @Test
    void classifyUserType_detectsDisciplinedSaver() {
        ProfileScoring.UserTypeResult result = ProfileScoring.classifyUserType(scores(
                "data_trust", 80,
                "liquidity_safety", 70,
                "spending_control", 75,
                "debt_pressure", 70,
                "income_stability", 70,
                "lifestyle_inflation", 70,
                "fixed_burden", 70,
                "savings_discipline", 80));
        assertEquals("disciplined_saver", result.type());
    }

    @Test
    void classifyUserType_detectsCashflowStressed() {
        ProfileScoring.UserTypeResult result = ProfileScoring.classifyUserType(scores(
                "data_trust", 80,
                "liquidity_safety", 30,
                "spending_control", 70));
        assertEquals("cashflow_stressed", result.type());
    }

    @Test
    void classifyUserType_detectsLifestyleInflation() {
        ProfileScoring.UserTypeResult result = ProfileScoring.classifyUserType(scores(
                "data_trust", 80,
                "liquidity_safety", 70,
                "spending_control", 70,
                "debt_pressure", 70,
                "income_stability", 70,
                "lifestyle_inflation", 30,
                "fixed_burden", 70));
        assertEquals("lifestyle_inflation", result.type());
    }

    @Test
    void concentrationFromRows_computesTopShare() {
        List<Map<String, Object>> rows = List.of(
                categoryRow("food", "Food", 4000),
                categoryRow("travel", "Travel", 1000));
        ProfileScoring.ConcentrationStats stats = ProfileScoring.concentrationFromRows(rows);
        assertEquals(80.0, stats.topSharePct());
        assertEquals("Food", stats.topCategoryName());
    }

    @Test
    void reasons_arePlainLanguage() {
        assertTrue(ProfileScoring.incomeStabilityReason(80, List.of(10000.0, 10100.0)).contains("steady"));
        assertTrue(ProfileScoring.spendingConcentrationReason(
                40, new ProfileScoring.ConcentrationStats(60, "food", "Food", 10000))
                .contains("60%"));
    }

    private static Map<String, Double> scores(Object... kv) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put(String.valueOf(kv[i]), ((Number) kv[i + 1]).doubleValue());
        }
        return out;
    }

    private static Map<String, Object> categoryRow(String code, String name, double amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("category_code", code);
        row.put("category_name", name);
        row.put("amount", amount);
        return row;
    }
}
