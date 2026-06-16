package com.finsight.application.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Scenario-scaled confidence intervals for forecast outputs.
 */
public final class ForecastConfidence {

    public record Spread(double lowerFactor, double upperFactor, double halfWidthPct) {
    }

    private ForecastConfidence() {
    }

    public static Spread forScenario(String scenario) {
        double half = switch (scenario == null ? "base" : scenario) {
            case "conservative" -> 0.12;
            case "optimistic" -> 0.08;
            case "stress" -> 0.15;
            default -> 0.10;
        };
        return new Spread(1 - half, 1 + half, round(half * 100));
    }

    public static double lower(double value, Spread spread) {
        return round(value * spread.lowerFactor());
    }

    public static double upper(double value, Spread spread) {
        return round(value * spread.upperFactor());
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
