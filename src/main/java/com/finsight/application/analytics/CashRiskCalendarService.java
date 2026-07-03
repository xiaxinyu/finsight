package com.finsight.application.analytics;

import com.finsight.application.finance.BillService;
import com.finsight.application.finance.FinancialGoalService;
import com.finsight.application.finance.UserFinancePreferencesService;
import com.finsight.domain.model.Bill;
import com.finsight.domain.model.FinancialGoal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class CashRiskCalendarService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int[] DEFAULT_INCOME_PAY_DAYS = {5, 20};

    private final ForecastService forecastService;
    private final BillService billService;
    private final FinancialGoalService goalService;
    private final UserFinancePreferencesService financePreferencesService;

    public CashRiskCalendarService(ForecastService forecastService,
                                   BillService billService,
                                   FinancialGoalService goalService,
                                   UserFinancePreferencesService financePreferencesService) {
        this.forecastService = forecastService;
        this.billService = billService;
        this.goalService = goalService;
        this.financePreferencesService = financePreferencesService;
    }

    public Map<String, Object> calendar(int year, String scenario) throws Exception {
        Map<String, Object> forecast = forecastService.forecast(year, scenario);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> forecastMonths = (List<Map<String, Object>>) forecast.getOrDefault("months", List.of());
        @SuppressWarnings("unchecked")
        Set<String> deficitMonths = new TreeSet<>((List<String>) forecast.getOrDefault("deficitMonths", List.of()));

        Map<String, Map<String, Object>> dayMap = new LinkedHashMap<>();
        List<Map<String, Object>> monthSummaries = new ArrayList<>();

        for (Map<String, Object> monthRow : forecastMonths) {
            String yearMonth = String.valueOf(monthRow.get("yearMonth"));
            double net = toDouble(monthRow.get("net"));
            String riskLevel = riskLevelForMonth(net, deficitMonths.contains(yearMonth));
            monthSummaries.add(Map.of(
                    "yearMonth", yearMonth,
                    "net", round(net),
                    "riskLevel", riskLevel
            ));

            double income = toDouble(monthRow.get("income"));
            YearMonth ym = YearMonth.parse(yearMonth);
            addIncomeEvents(dayMap, ym, income);
        }

        for (Bill bill : billService.listEnabled()) {
            if (bill.getDueDay() == null || bill.getAmount() == null) {
                continue;
            }
            for (int month = 1; month <= 12; month++) {
                YearMonth ym = YearMonth.of(year, month);
                int dueDay = Math.min(bill.getDueDay(), ym.lengthOfMonth());
                addEvent(dayMap, ym.atDay(dueDay), "bill", bill.getName(), bill.getAmount().doubleValue());
            }
        }

        for (FinancialGoal goal : goalService.list()) {
            if (goal.getMonthlyContribution() == null || goal.getMonthlyContribution().doubleValue() <= 0) {
                continue;
            }
            for (int month = 1; month <= 12; month++) {
                YearMonth ym = YearMonth.of(year, month);
                addEvent(dayMap, ym.atDay(1), "goal", goal.getName(), goal.getMonthlyContribution().doubleValue());
            }
        }

        annotateDayRisk(dayMap, deficitMonths);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", year);
        out.put("scenario", scenario);
        out.put("deficitMonths", new ArrayList<>(deficitMonths));
        out.put("months", monthSummaries);
        out.put("days", new ArrayList<>(dayMap.values()));
        out.put("metricsGate", forecast.get("metricsGate"));
        out.put("metricsSource", forecast.get("metricsSource"));
        int[] payDays = resolveIncomePayDays();
        out.put("incomePayDays", java.util.Arrays.stream(payDays).boxed().toList());
        return out;
    }

    int[] resolveIncomePayDays() {
        try {
            int[] configured = financePreferencesService.incomePayDays();
            return configured == null || configured.length == 0 ? DEFAULT_INCOME_PAY_DAYS : configured;
        } catch (Exception ex) {
            return DEFAULT_INCOME_PAY_DAYS;
        }
    }

    static String riskLevelForMonth(double net, boolean deficitMonth) {
        if (deficitMonth || net < 0) {
            return "high";
        }
        if (net < 500) {
            return "medium";
        }
        return "low";
    }

    static String riskLevelForDay(double inflow, double outflow, boolean deficitMonth) {
        double net = inflow - outflow;
        if (deficitMonth && outflow > 0 && net < 0) {
            return "high";
        }
        if (outflow > inflow && outflow > 0) {
            return "medium";
        }
        if (deficitMonth && outflow > 0) {
            return "medium";
        }
        return "low";
    }

    private void addIncomeEvents(Map<String, Map<String, Object>> dayMap, YearMonth ym, double income) {
        if (income <= 0) {
            return;
        }
        int[] payDays = resolveIncomePayDays();
        double portion = income / payDays.length;
        for (int payDay : payDays) {
            int day = Math.min(payDay, ym.lengthOfMonth());
            addEvent(dayMap, ym.atDay(day), "income", "Estimated income", portion);
        }
    }

    private void annotateDayRisk(Map<String, Map<String, Object>> dayMap, Set<String> deficitMonths) {
        for (Map<String, Object> day : dayMap.values()) {
            String date = String.valueOf(day.get("date"));
            String yearMonth = date.substring(0, 7);
            double inflow = toDouble(day.get("inflow"));
            double outflow = toDouble(day.get("outflow"));
            day.put("riskLevel", riskLevelForDay(inflow, outflow, deficitMonths.contains(yearMonth)));
        }
    }

    private void addEvent(Map<String, Map<String, Object>> dayMap,
                          LocalDate date,
                          String type,
                          String label,
                          double amount) {
        String key = date.format(ISO_DATE);
        Map<String, Object> day = dayMap.computeIfAbsent(key, d -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", key);
            row.put("inflow", 0.0);
            row.put("outflow", 0.0);
            row.put("events", new ArrayList<Map<String, Object>>());
            row.put("riskLevel", "low");
            return row;
        });

        if ("income".equals(type)) {
            day.put("inflow", round(toDouble(day.get("inflow")) + amount));
        } else {
            day.put("outflow", round(toDouble(day.get("outflow")) + amount));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) day.get("events");
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("label", label);
        event.put("amount", round(amount));
        events.add(event);
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
