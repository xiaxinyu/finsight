package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.BillService;
import com.finsight.common.exception.AppServiceException;
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

    public static final String METRIC_INCOME_FORECAST = "INCOME_FORECAST";
    public static final String METRIC_EXPENSE_FORECAST = "EXPENSE_FORECAST";
    public static final String METRIC_NET_FORECAST = "NET_FORECAST";

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
            row.put("incomeLower", round(income * 0.9));
            row.put("incomeUpper", round(income * 1.1));
            row.put("expenseLower", round(expense * 0.9));
            row.put("expenseUpper", round(expense * 1.1));
            row.put("netLower", round(net * 0.9));
            row.put("netUpper", round(net * 1.1));
            row.put("deficit", net < 0);
            row.put("forecast", true);
            months.add(row);
        }

        String runId = UUID.randomUUID().toString();
        jdbcTemplate.update("insert into fin_forecast_run (id, user_id, scenario, target_year, params_json, created_at) "
                        + "values (?, ?, ?, ?, ?, now(3))",
                runId, userId, scenario, year, "{\"method\":\"rolling_mean_seasonal\"}");
        persistForecastLines(runId, months);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", year);
        out.put("scenario", scenario);
        out.put("runId", runId);
        out.put("yearIncome", round(yearIncome));
        out.put("yearExpense", round(yearExpense));
        out.put("yearNet", round(yearIncome - yearExpense));
        out.put("deficitMonths", deficitMonths);
        out.put("months", months);
        out.put("budgetSuggestion", buildBudgetSuggestion(yearExpense, scenario, deficitMonths.size()));
        out.put("metricsGate", metricsGate);
        out.put("metricsSource", reportFallback ? "report_sql" : "fin_metric_monthly");
        return out;
    }

    public List<Map<String, Object>> forecastLines(String runId) throws AppServiceException {
        String userId = userKey();
        if (!tableExists("fin_forecast_line")) {
            throw new AppServiceException("Forecast lines are not available");
        }
        Integer owned = jdbcTemplate.queryForObject(
                "select count(*) from fin_forecast_run where id = ? and user_id = ?",
                Integer.class,
                runId,
                userId);
        if (owned == null || owned == 0) {
            throw new AppServiceException("Forecast run not found");
        }
        return jdbcTemplate.query(
                "select month_key, metric_code, metric_value, lower_bound, upper_bound "
                        + "from fin_forecast_line where run_id = ? order by month_key, metric_code",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("monthKey", rs.getString("month_key"));
                    row.put("metricCode", rs.getString("metric_code"));
                    row.put("metricValue", rs.getBigDecimal("metric_value"));
                    row.put("lowerBound", rs.getBigDecimal("lower_bound"));
                    row.put("upperBound", rs.getBigDecimal("upper_bound"));
                    return row;
                },
                runId);
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

    private static Map<String, Object> buildBudgetSuggestion(double yearExpense, String scenario, int deficitCount) {
        double monthlyAvg = yearExpense / 12;
        double buffer = budgetBuffer(scenario, deficitCount);
        double monthlyCap = Math.ceil(monthlyAvg * buffer);
        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("monthlyCap", round(monthlyCap));
        suggestion.put("annualCap", round(monthlyCap * 12));
        suggestion.put("note", budgetNote(scenario, deficitCount));
        return suggestion;
    }

    private static double budgetBuffer(String scenario, int deficitCount) {
        double base = switch (scenario == null ? "base" : scenario) {
            case "conservative" -> 1.05;
            case "optimistic" -> 0.98;
            case "stress" -> 1.10;
            default -> 1.0;
        };
        if (deficitCount > 0) {
            base += 0.03;
        }
        return base;
    }

    private static String budgetNote(String scenario, int deficitCount) {
        String scen = scenario == null ? "base" : scenario;
        if (deficitCount > 0) {
            return "Suggested cap from " + scen + " forecast with a small cushion for "
                    + deficitCount + " projected deficit month(s).";
        }
        return "Suggested monthly cap from average projected expense under the " + scen + " scenario.";
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void persistForecastLines(String runId, List<Map<String, Object>> months) {
        if (!tableExists("fin_forecast_line")) {
            return;
        }
        for (Map<String, Object> month : months) {
            String monthKey = String.valueOf(month.get("yearMonth"));
            insertForecastLine(runId, monthKey, METRIC_INCOME_FORECAST, ((Number) month.get("income")).doubleValue());
            insertForecastLine(runId, monthKey, METRIC_EXPENSE_FORECAST, ((Number) month.get("expense")).doubleValue());
            insertForecastLine(runId, monthKey, METRIC_NET_FORECAST, ((Number) month.get("net")).doubleValue());
        }
    }

    private void insertForecastLine(String runId, String monthKey, String metricCode, double value) {
        BigDecimal metricValue = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
        BigDecimal lower = metricValue.multiply(BigDecimal.valueOf(0.9)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal upper = metricValue.multiply(BigDecimal.valueOf(1.1)).setScale(4, RoundingMode.HALF_UP);
        jdbcTemplate.update(
                "insert into fin_forecast_line (id, run_id, month_key, metric_code, metric_value, lower_bound, upper_bound) "
                        + "values (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                runId,
                monthKey,
                metricCode,
                metricValue,
                lower,
                upper);
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = ?",
                Integer.class,
                table);
        return count != null && count > 0;
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
