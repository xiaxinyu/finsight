package com.finsight.application.analytics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional what-if adjustments applied on top of the rolling-mean seasonal forecast.
 */
public record ForecastScenarioParams(
        Double incomeChangePct,
        Double newMonthlyBill,
        Double lumpSumExpense,
        Double targetMonthlyPayment
) {

    public static ForecastScenarioParams empty() {
        return new ForecastScenarioParams(null, null, null, null);
    }

    public static ForecastScenarioParams fromMap(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return empty();
        }
        return new ForecastScenarioParams(
                readDouble(params.get("incomeChangePct")),
                readDouble(params.get("newMonthlyBill")),
                readDouble(params.get("lumpSumExpense")),
                readDouble(params.get("targetMonthlyPayment"))
        );
    }

    public boolean hasAdjustments() {
        return incomeChangePct != null || newMonthlyBill != null || lumpSumExpense != null || targetMonthlyPayment != null;
    }

    public List<String> explanationLines() {
        List<String> lines = new ArrayList<>();
        if (incomeChangePct != null && incomeChangePct != 0) {
            lines.add(String.format("Income adjusted by %+.1f%% across all forecast months.", incomeChangePct));
        }
        if (newMonthlyBill != null && newMonthlyBill > 0) {
            lines.add(String.format("Added %s/month recurring bill to every forecast month.", formatMoney(newMonthlyBill)));
        }
        if (lumpSumExpense != null && lumpSumExpense > 0) {
            lines.add(String.format("Applied %s one-time expense in January.", formatMoney(lumpSumExpense)));
        }
        if (targetMonthlyPayment != null && targetMonthlyPayment > 0) {
            lines.add(String.format("Budget comparison uses %s/month target payment.", formatMoney(targetMonthlyPayment)));
        }
        return lines;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        if (incomeChangePct != null) {
            out.put("incomeChangePct", incomeChangePct);
        }
        if (newMonthlyBill != null) {
            out.put("newMonthlyBill", newMonthlyBill);
        }
        if (lumpSumExpense != null) {
            out.put("lumpSumExpense", lumpSumExpense);
        }
        if (targetMonthlyPayment != null) {
            out.put("targetMonthlyPayment", targetMonthlyPayment);
        }
        return out;
    }

    private static Double readDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    private static String formatMoney(double amount) {
        return String.format("%.2f", amount);
    }
}
