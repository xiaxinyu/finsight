package com.finsight.application.finance;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InsightService {

    private final WealthService wealthService;
    private final CashflowService cashflowService;
    private final BudgetService budgetService;

    public InsightService(WealthService wealthService,
                          CashflowService cashflowService,
                          BudgetService budgetService) {
        this.wealthService = wealthService;
        this.cashflowService = cashflowService;
        this.budgetService = budgetService;
    }

    public List<Map<String, Object>> decisionCards() {
        List<Map<String, Object>> cards = new ArrayList<>();
        Map<String, Object> wealth = wealthService.snapshot();
        Map<String, Object> cashflow = cashflowService.metrics();

        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) wealth.get("healthScore");
        double fixedBurden = ((Number) health.get("fixedBurden")).doubleValue();
        if (fixedBurden > 35) {
            cards.add(card("warning", "Fixed costs are " + Math.round(fixedBurden) + "% of income. Aim for 35% or below.",
                    "/planning"));
        }

        double runway = ((Number) cashflow.get("runwayMonths")).doubleValue();
        if (runway < 3) {
            cards.add(card("warning", "Liquidity covers only " + String.format("%.1f", runway) + " months. Build emergency fund first.",
                    "/wealth"));
        }

        double savingsRate = ((Number) wealth.get("savingsRate")).doubleValue();
        if (savingsRate >= 0.2) {
            cards.add(card("info", "Savings rate is " + Math.round(savingsRate * 100) + "% — strong discipline.",
                    "/reports/cashflow"));
        }

        List<Map<String, Object>> bva = budgetService.budgetVsActual();
        if (!bva.isEmpty()) {
            double actual = ((Number) bva.get(0).get("actualTotal")).doubleValue();
            double limit = ((Number) bva.get(0).get("limitTotal")).doubleValue();
            if (limit > 0 && actual / limit > 0.8) {
                cards.add(card("warning", "Monthly spending is above 80% of budget.", "/planning"));
            }
        }

        if (cards.isEmpty()) {
            cards.add(card("info", "Finances look stable. Review cashflow and set a savings goal.", "/goals"));
        }
        return cards;
    }

    private static Map<String, Object> card(String type, String text, String actionPath) {
        return Map.of("type", type, "text", text, "actionPath", actionPath);
    }
}
