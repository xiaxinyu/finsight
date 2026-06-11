package com.finsight.application.finance;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InsightService {

    private final WealthService wealthService;
    private final CashflowService cashflowService;
    private final BudgetService budgetService;
    private final DataQualityService dataQualityService;

    public InsightService(WealthService wealthService,
                          CashflowService cashflowService,
                          BudgetService budgetService,
                          DataQualityService dataQualityService) {
        this.wealthService = wealthService;
        this.cashflowService = cashflowService;
        this.budgetService = budgetService;
        this.dataQualityService = dataQualityService;
    }

    public List<Map<String, Object>> decisionCards() {
        List<Map<String, Object>> cards = new ArrayList<>();
        Map<String, Object> wealth = wealthService.snapshot();
        Map<String, Object> cashflow = cashflowService.metrics();
        Map<String, Object> quality = dataQualityService.summary();

        int unclassified = ((Number) quality.getOrDefault("unclassifiedCount", 0)).intValue();
        if (unclassified > 0) {
            cards.add(card("warning", "Unclassified transactions",
                    unclassified + " transactions need a category before reports are reliable.",
                    "/transactions?unclassified=1", "Review transactions", unclassified, 0));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) wealth.get("healthScore");
        double fixedBurden = ((Number) health.get("fixedBurden")).doubleValue();
        if (fixedBurden > 35) {
            cards.add(card("warning", "High fixed burden",
                    "Fixed costs are " + Math.round(fixedBurden) + "% of income. Target 35% or below.",
                    "/planning", "Adjust budget", fixedBurden, 35));
        }

        double runway = ((Number) cashflow.get("runwayMonths")).doubleValue();
        if (runway < 3) {
            cards.add(card("warning", "Low emergency runway",
                    "Liquidity covers only " + String.format("%.1f", runway) + " months of spending.",
                    "/wealth", "Build emergency fund", runway, 3));
        }

        double savingsRate = ((Number) wealth.get("savingsRate")).doubleValue();
        if (savingsRate >= 0.2) {
            cards.add(card("info", "Strong savings rate (YTD)",
                    "Year-to-date savings rate is " + Math.round(savingsRate * 100) + "% — keep the momentum.",
                    "/reports/cashflow", "View cashflow", savingsRate * 100, 20));
        } else if (savingsRate < 0) {
            cards.add(card("warning", "Negative savings (YTD)",
                    "Year-to-date spending exceeds income by " + Math.round(Math.abs(savingsRate) * 100) + "% of income.",
                    "/reports/cashflow", "View cashflow", savingsRate * 100, 0));
        }

        List<Map<String, Object>> bva = budgetService.budgetVsActual();
        if (!bva.isEmpty()) {
            double actual = ((Number) bva.get(0).get("actualTotal")).doubleValue();
            double limit = ((Number) bva.get(0).get("limitTotal")).doubleValue();
            if (limit > 0 && actual / limit > 0.8) {
                double remaining = limit - actual;
                cards.add(card("warning", "Budget pressure",
                        "Monthly spending is above 80% of budget. About " + Math.round(remaining) + " CNY left.",
                        "/planning", "Adjust budget", actual / limit * 100, 80));
            }
        }

        if (cards.isEmpty()) {
            cards.add(card("info", "Finances look stable",
                    "Review cashflow trends and set a savings goal when ready.",
                    "/goals", "Set a goal", 0, 0));
        }
        return cards;
    }

    private static Map<String, Object> card(String type, String title, String detail,
                                              String actionPath, String actionLabel,
                                              double metric, double threshold) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("severity", type);
        m.put("title", title);
        m.put("detail", detail);
        m.put("text", title + ": " + detail);
        m.put("actionPath", actionPath);
        m.put("actionLabel", actionLabel);
        m.put("metric", metric);
        m.put("threshold", threshold);
        return m;
    }
}
