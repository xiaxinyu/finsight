package com.finsight.application.analytics;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid forecast projection: rolling window + calendar-month seasonality + recent trend.
 */
public final class ForecastProjection {

    public record YearMonthValue(YearMonth ym, double value) {
    }

    public record Quality(int sampleMonths, String confidenceLevel, boolean seasonalAllowed) {
    }

    private ForecastProjection() {
    }

    public static Quality assessQuality(int sampleMonths) {
        if (sampleMonths < 3) {
            return new Quality(sampleMonths, "low", false);
        }
        if (sampleMonths < 6) {
            return new Quality(sampleMonths, "low", false);
        }
        if (sampleMonths < 12) {
            return new Quality(sampleMonths, "medium", false);
        }
        return new Quality(sampleMonths, "high", true);
    }

    public static double rollingMean(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        int window = Math.min(6, values.size());
        return values.subList(values.size() - window, values.size()).stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    public static double calendarMonthSeasonal(int calendarMonth, List<YearMonthValue> history) {
        if (history.size() < 12) {
            return 1.0;
        }
        double monthAvg = history.stream()
                .filter(h -> h.ym().getMonthValue() == calendarMonth)
                .mapToDouble(YearMonthValue::value)
                .average()
                .orElse(0);
        double overall = history.stream().mapToDouble(YearMonthValue::value).average().orElse(0);
        if (overall <= 0 || monthAvg <= 0) {
            return 1.0;
        }
        double factor = monthAvg / overall;
        return Math.max(0.7, Math.min(1.3, factor));
    }

    public static double trendMultiplier(List<Double> values) {
        if (values.size() < 6) {
            return 1.0;
        }
        double last3 = average(values.subList(values.size() - 3, values.size()));
        double prior3 = average(values.subList(values.size() - 6, values.size() - 3));
        if (prior3 <= 0) {
            return 1.0;
        }
        double growth = (last3 - prior3) / prior3;
        return 1.0 + Math.max(-0.15, Math.min(0.15, growth * 0.5));
    }

    public static double projectMonth(List<Double> priorValues,
                                      int calendarMonth,
                                      List<YearMonthValue> fullHistory,
                                      double scenarioFactor) {
        if (priorValues.isEmpty()) {
            return 0;
        }
        Quality quality = assessQuality(priorValues.size());
        double base = rollingMean(priorValues);
        double seasonal = quality.seasonalAllowed()
                ? calendarMonthSeasonal(calendarMonth, fullHistory)
                : 1.0;
        double trend = trendMultiplier(priorValues);
        return base * seasonal * trend * scenarioFactor;
    }

    public static List<YearMonthValue> toSeries(Map<String, Double> byMonthKey) {
        List<YearMonthValue> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : new LinkedHashMap<>(byMonthKey).entrySet()) {
            out.add(new YearMonthValue(YearMonth.parse(e.getKey()), e.getValue()));
        }
        out.sort((a, b) -> a.ym().compareTo(b.ym()));
        return out;
    }

    public static List<Double> priorValues(List<YearMonthValue> series, YearMonth before) {
        return series.stream()
                .filter(p -> p.ym().isBefore(before))
                .map(YearMonthValue::value)
                .toList();
    }

    private static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
