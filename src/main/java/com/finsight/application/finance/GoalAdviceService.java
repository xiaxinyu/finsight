package com.finsight.application.finance;

import com.finsight.application.analytics.ForecastService;
import com.finsight.domain.model.FinancialGoal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoalAdviceService {

    private final ForecastService forecastService;
    private final FinancialGoalService goalService;

    public GoalAdviceService(ForecastService forecastService, FinancialGoalService goalService) {
        this.forecastService = forecastService;
        this.goalService = goalService;
    }

    public Map<String, Object> advise(String goalId) throws Exception {
        FinancialGoal goal = goalService.list().stream()
                .filter(g -> goalId.equals(g.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        Map<String, Object> progress = goalService.progress(goal);
        BigDecimal target = goal.getTargetAmount() == null ? BigDecimal.ZERO : goal.getTargetAmount();
        BigDecimal current = goal.getCurrentAmount() == null ? BigDecimal.ZERO : goal.getCurrentAmount();
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO);

        int monthsToTarget = monthsUntil(goal.getTargetDate());
        BigDecimal recommendedMonthly = monthsToTarget > 0
                ? remaining.divide(BigDecimal.valueOf(monthsToTarget), 2, RoundingMode.CEILING)
                : remaining;

        int year = goal.getTargetDate() != null
                ? YearMonth.from(goal.getTargetDate().toInstant().atZone(java.time.ZoneId.systemDefault())).getYear()
                : YearMonth.now().getYear();
        Map<String, Object> forecast = forecastService.forecast(year, "base");
        double yearNet = ((Number) forecast.getOrDefault("yearNet", 0)).doubleValue();
        BigDecimal contribution = goal.getMonthlyContribution() == null ? recommendedMonthly : goal.getMonthlyContribution();
        double required = contribution.doubleValue() * Math.max(1, monthsToTarget);
        double successProbability = yearNet <= 0 ? 0.35
                : Math.min(0.95, Math.max(0.15, yearNet / Math.max(required, 1)));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("goal", goal);
        out.put("progress", progress);
        out.put("recommendedMonthly", recommendedMonthly);
        out.put("monthsToTarget", monthsToTarget);
        out.put("successProbability", BigDecimal.valueOf(successProbability).setScale(2, RoundingMode.HALF_UP));
        out.put("forecastYearNet", yearNet);
        @SuppressWarnings("unchecked")
        List<String> deficitMonths = (List<String>) forecast.getOrDefault("deficitMonths", List.of());
        out.put("cashflowRiskMonths", deficitMonths.size());
        out.put("metricsSource", forecast.get("metricsSource"));
        return out;
    }

    private static int monthsUntil(java.util.Date targetDate) {
        if (targetDate == null) {
            return 12;
        }
        LocalDate target = targetDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        long months = ChronoUnit.MONTHS.between(YearMonth.now().atDay(1), target.withDayOfMonth(1));
        return (int) Math.max(1, months);
    }
}
