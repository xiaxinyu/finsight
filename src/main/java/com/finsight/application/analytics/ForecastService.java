package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.BillService;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ForecastService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MetricMonthlyRepository metricRepository;
    private final BillService billService;
    private final AuthenticationFacade authenticationFacade;
    private final JdbcTemplate jdbcTemplate;
    private final MetricGateService metricGateService;
    private final MetricMonthlyService metricMonthlyService;

    public ForecastService(MetricMonthlyRepository metricRepository,
                           BillService billService,
                           AuthenticationFacade authenticationFacade,
                           JdbcTemplate jdbcTemplate,
                           MetricGateService metricGateService,
                           MetricMonthlyService metricMonthlyService) {
        this.metricRepository = metricRepository;
        this.billService = billService;
        this.authenticationFacade = authenticationFacade;
        this.jdbcTemplate = jdbcTemplate;
        this.metricGateService = metricGateService;
        this.metricMonthlyService = metricMonthlyService;
    }

    public Map<String, Object> forecast(int year, String scenario) throws Exception {
        String userId = userKey();
        YearMonth end = YearMonth.now().minusMonths(1);
        YearMonth start = end.minusMonths(23);
        Map<String, Object> metricsGate = metricGateService.status(3);
        boolean reportFallback = metricGateService.useReportFallback();
        List<Map<String, Object>> history = reportFallback
                ? metricMonthlyService.historyFromReports(start.format(YM), end.format(YM))
                : metricRepository.listForUser(userId, start.format(YM), end.format(YM));

        double avgIncome = rollingAvg(history, MetricCode.INCOME_TOTAL.name());
        double avgExpense = rollingAvg(history, MetricCode.EXPENSE_TOTAL.name());
        double factor = scenarioFactor(scenario);

        List<Map<String, Object>> months = new ArrayList<>();
        double yearIncome = 0;
        double yearExpense = 0;
        List<String> deficitMonths = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            double seasonal = seasonalIndex(m);
            double income = avgIncome * seasonal * factor;
            double expense = avgExpense * seasonal * (2 - factor * 0.5);
            expense += billsForMonth(m);
            double net = income - expense;
            yearIncome += income;
            yearExpense += expense;
            if (net < 0) {
                deficitMonths.add(ym.format(YM));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("yearMonth", ym.format(YM));
            row.put("income", round(income));
            row.put("expense", round(expense));
            row.put("net", round(net));
            row.put("forecast", true);
            months.add(row);
        }

        String runId = UUID.randomUUID().toString();
        jdbcTemplate.update("insert into fin_forecast_run (id, user_id, scenario, target_year, params_json, created_at) "
                        + "values (?, ?, ?, ?, ?, now(3))",
                runId, userId, scenario, year, "{\"method\":\"rolling_mean_seasonal\"}");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", year);
        out.put("scenario", scenario);
        out.put("runId", runId);
        out.put("yearIncome", round(yearIncome));
        out.put("yearExpense", round(yearExpense));
        out.put("yearNet", round(yearIncome - yearExpense));
        out.put("deficitMonths", deficitMonths);
        out.put("months", months);
        out.put("metricsGate", metricsGate);
        out.put("metricsSource", reportFallback ? "report_sql" : "fin_metric_monthly");
        return out;
    }

    public Map<String, Object> simulateScenario(Map<String, Object> params) throws Exception {
        int year = ((Number) params.getOrDefault("year", YearMonth.now().getYear())).intValue();
        String scenario = String.valueOf(params.getOrDefault("scenario", "base"));
        if (params.get("incomeChangePct") instanceof Number pct && ((Number) params.get("incomeChangePct")).doubleValue() < -5) {
            scenario = "stress";
        } else if (params.get("newMonthlyBill") instanceof Number bill && ((Number) params.get("newMonthlyBill")).doubleValue() > 0) {
            scenario = "conservative";
        }
        Map<String, Object> out = forecast(year, scenario);
        out.put("inputParams", params);
        return out;
    }

    private double billsForMonth(int month) {
        return billService.listEnabled().stream()
                .filter(b -> b.getAmount() != null)
                .mapToDouble(b -> b.getAmount().doubleValue())
                .sum();
    }

    private static double rollingAvg(List<Map<String, Object>> history, String code) {
        List<Double> vals = new ArrayList<>();
        for (Map<String, Object> row : history) {
            if (code.equals(String.valueOf(row.get("metricCode")))) {
                vals.add(((Number) row.get("metricValue")).doubleValue());
            }
        }
        if (vals.isEmpty()) {
            return 0;
        }
        int window = Math.min(6, vals.size());
        return vals.subList(vals.size() - window, vals.size()).stream().mapToDouble(d -> d).average().orElse(0);
    }

    private static double seasonalIndex(int month) {
        return 1.0 + 0.05 * Math.sin((month - 1) * Math.PI / 6);
    }

    private static double scenarioFactor(String scenario) {
        return switch (scenario == null ? "base" : scenario) {
            case "conservative" -> 0.92;
            case "optimistic" -> 1.08;
            case "stress" -> 0.85;
            default -> 1.0;
        };
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
