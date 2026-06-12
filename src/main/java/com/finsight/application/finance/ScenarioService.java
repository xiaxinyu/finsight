package com.finsight.application.finance;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @deprecated Prefer {@link com.finsight.application.analytics.ForecastService} via /api/v1/analytics/scenarios.
 */
@Deprecated
@Service
public class ScenarioService {

    private final WealthService wealthService;
    private final CashflowService cashflowService;

    public ScenarioService(WealthService wealthService, CashflowService cashflowService) {
        this.wealthService = wealthService;
        this.cashflowService = cashflowService;
    }

    public Map<String, Object> simulate(double lumpSumExpense, double incomeChangePct, double newMonthlyBill) {
        Map<String, Object> baseline = wealthService.snapshot();
        Map<String, Object> cashflow = cashflowService.metrics();

        double netWorth = ((Number) baseline.get("netWorth")).doubleValue();
        double safeToSpend = ((Number) cashflow.get("safeToSpend")).doubleValue();
        double runway = ((Number) cashflow.get("runwayMonths")).doubleValue();

        double scenarioNetWorth = netWorth - lumpSumExpense;
        double scenarioSafe = safeToSpend - lumpSumExpense;
        double incomeEffect = netWorth * (incomeChangePct / 100.0);
        scenarioNetWorth += incomeEffect;
        double billEffect = newMonthlyBill * 12;
        scenarioSafe -= billEffect;
        double scenarioRunway = runway;
        if (newMonthlyBill > 0 && runway > 0) {
            scenarioRunway = Math.max(0, runway - (newMonthlyBill / Math.max(1, ((Number) cashflow.get("burnRateDaily")).doubleValue() * 30)));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("baseline", Map.of(
                "netWorth", netWorth,
                "safeToSpend", safeToSpend,
                "runwayMonths", runway
        ));
        out.put("scenario", Map.of(
                "netWorth", scenarioNetWorth,
                "safeToSpend", scenarioSafe,
                "runwayMonths", scenarioRunway
        ));
        out.put("delta", Map.of(
                "netWorth", scenarioNetWorth - netWorth,
                "safeToSpend", scenarioSafe - safeToSpend,
                "runwayMonths", scenarioRunway - runway
        ));
        return out;
    }
}
