package com.finsight.application.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure helpers for YoY trend decomposition and contribution math.
 */
public final class TrendDecomposition {

    private TrendDecomposition() {
    }

    public static double pctChange(double from, double to) {
        if (from <= 0) {
            return to > 0 ? 100.0 : 0.0;
        }
        return (to - from) / from * 100.0;
    }

    public static double contributionPct(double delta, double totalDelta) {
        if (totalDelta == 0) {
            return 0;
        }
        return delta / totalDelta * 100.0;
    }

    public static boolean lifestyleInflationDetected(double incomePct, double expensePct, double expenseDelta) {
        return expenseDelta > 0 && expensePct - incomePct >= 5.0;
    }

    public static Map<String, Object> deltaMetric(double from, double to) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", round(from));
        m.put("to", round(to));
        m.put("deltaAmount", round(to - from));
        m.put("deltaPercent", round(pctChange(from, to)));
        return m;
    }

    public static Map<String, Object> trendItem(String type,
                                                String label,
                                                double deltaAmount,
                                                double deltaPercent,
                                                double contributionPct,
                                                Map<String, String> drillDown) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("label", label);
        item.put("deltaAmount", round(deltaAmount));
        item.put("deltaPercent", round(deltaPercent));
        item.put("contributionPct", round(contributionPct));
        item.put("drillDown", drillDown);
        return item;
    }

    public static List<Map<String, Object>> topMovers(Map<String, Double> from,
                                                      Map<String, Double> to,
                                                      Map<String, String> labels,
                                                      double totalDelta,
                                                      int limit) {
        List<Map<String, Object>> movers = new ArrayList<>();
        for (Map.Entry<String, Double> entry : to.entrySet()) {
            String key = entry.getKey();
            double start = from.getOrDefault(key, 0.0);
            double end = entry.getValue();
            double delta = end - start;
            if (Math.abs(delta) < 1) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", key);
            row.put("label", labels.getOrDefault(key, key));
            row.put("deltaAmount", round(delta));
            row.put("deltaPercent", round(pctChange(start, end)));
            row.put("contributionPct", round(contributionPct(delta, totalDelta)));
            row.put("fromAmount", round(start));
            row.put("toAmount", round(end));
            movers.add(row);
        }
        movers.sort(Comparator.comparingDouble((Map<String, Object> r) ->
                Math.abs(((Number) r.get("deltaAmount")).doubleValue())).reversed());
        return movers.size() > limit ? movers.subList(0, limit) : movers;
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
