package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.BillService;
import com.finsight.application.finance.BudgetService;
import com.finsight.common.exception.AppServiceException;
import com.finsight.domain.model.BudgetLine;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.Objects;
import java.util.UUID;

@Service
public class ForecastService {

    public static final String METRIC_INCOME_FORECAST = "INCOME_FORECAST";
    public static final String METRIC_EXPENSE_FORECAST = "EXPENSE_FORECAST";
    public static final String METRIC_NET_FORECAST = "NET_FORECAST";
    public static final String METRIC_CATEGORY_EXPENSE_PREFIX = "EXPENSE_FORECAST_CAT:";

    private static final int TOP_CATEGORY_FORECASTS = 5;
    private static final long FORECAST_BUDGET_MS = 1000;

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final String PREVIEW_RUN_PREFIX = "preview-";

    private final MetricMonthlyRepository metricRepository;
    private final BillService billService;
    private final BudgetService budgetService;
    private final AuthenticationFacade authenticationFacade;
    private final JdbcTemplate jdbcTemplate;
    private final MetricGateService metricGateService;
    private final AnalyticsCacheService cacheService;
    private final AnalyticsRequestMemo requestMemo;
    private final AnalyticsCacheKeySupport cacheKeySupport;
    private final MetricGateRepairService metricGateRepairService;

    public ForecastService(MetricMonthlyRepository metricRepository,
                           BillService billService,
                           BudgetService budgetService,
                           AuthenticationFacade authenticationFacade,
                           JdbcTemplate jdbcTemplate,
                           MetricGateService metricGateService,
                           AnalyticsCacheService cacheService,
                           AnalyticsRequestMemo requestMemo,
                           AnalyticsCacheKeySupport cacheKeySupport,
                           MetricGateRepairService metricGateRepairService) {
        this.metricRepository = metricRepository;
        this.billService = billService;
        this.budgetService = budgetService;
        this.authenticationFacade = authenticationFacade;
        this.jdbcTemplate = jdbcTemplate;
        this.metricGateService = metricGateService;
        this.cacheService = cacheService;
        this.requestMemo = requestMemo;
        this.cacheKeySupport = cacheKeySupport;
        this.metricGateRepairService = metricGateRepairService;
    }

    public Map<String, Object> forecast(int year, String scenario) throws Exception {
        return forecastPreview(year, scenario, ForecastScenarioParams.empty());
    }

    public Map<String, Object> forecast(int year, String scenario, ForecastScenarioParams adjustments) throws Exception {
        return forecastPreview(year, scenario, adjustments);
    }

    private Map<String, Object> forecastPreview(int year, String scenario, ForecastScenarioParams adjustments)
            throws Exception {
        String cacheKey = cacheKeySupport.forecastKey(userKey(), year, scenario, adjustments);
        Map<String, Object> memo = requestMemo.getForecast(cacheKey);
        if (memo != null) {
            AnalyticsTiming.logCacheHit("forecast", true);
            return memo;
        }
        Map<String, Object> cached = cacheService.getForecast(cacheKey);
        if (cached != null) {
            AnalyticsTiming.logCacheHit("forecast", true);
            requestMemo.setForecast(cacheKey, cached);
            return cached;
        }
        try (AnalyticsTiming.TimedCall ignored = AnalyticsTiming.start("forecast", FORECAST_BUDGET_MS)) {
            Map<String, Object> computed = computeForecast(year, scenario, adjustments, false);
            cacheService.putForecast(cacheKey, computed);
            requestMemo.setForecast(cacheKey, computed);
            AnalyticsTiming.logCacheHit("forecast", false);
            return computed;
        }
    }

    Map<String, Object> computeForecast(int year, String scenario, ForecastScenarioParams adjustments, boolean persist)
            throws Exception {
        String userId = userKey();
        YearMonth end = YearMonth.now().minusMonths(1);
        YearMonth start = end.minusMonths(23);
        Map<String, Object> metricsGate = metricGateService.status(3);
        boolean gateMismatch = metricGateService.useReportFallback();
        List<Map<String, Object>> history = metricRepository.listForUser(userId, start.format(YM), end.format(YM));
        String metricsSource = gateMismatch ? "fin_metric_monthly_degraded" : "fin_metric_monthly";
        if (gateMismatch) {
            metricsGate = new LinkedHashMap<>(metricsGate);
            metricsGate.put("fallbackBlocked", true);
            metricsGate.put("warning",
                    "Reconciliation mismatch detected; inline report recalculation is disabled on read paths.");
            metricGateRepairService.scheduleRepairIfGateBlocked(true);
        }

        Map<String, Double> actualIncomeByMonth = monthlyMetricMapPreferring(
                history, MetricCode.REAL_INCOME.name(), MetricCode.INCOME_TOTAL.name());
        Map<String, Double> actualExpenseByMonth = monthlyMetricMapPreferring(
                history, MetricCode.CONSUMPTION_EXPENSE.name(), MetricCode.EXPENSE_TOTAL.name());
        List<ForecastProjection.YearMonthValue> incomeSeries = ForecastProjection.toSeries(actualIncomeByMonth);
        List<ForecastProjection.YearMonthValue> expenseSeries = ForecastProjection.toSeries(actualExpenseByMonth);
        ForecastProjection.Quality projectionQuality = ForecastProjection.assessQuality(incomeSeries.size());
        double factor = scenarioFactor(scenario);
        ForecastConfidence.Spread spread = ForecastConfidence.forScenario(scenario);
        double monthlyBudgetTarget = resolveMonthlyBudgetTarget(adjustments);

        List<Map<String, Object>> months = new ArrayList<>();
        double yearIncome = 0;
        double yearExpense = 0;
        List<String> deficitMonths = new ArrayList<>();
        int actualMonthCount = 0;

        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            String monthKey = ym.format(YM);
            boolean useActual = isCompletedMonth(ym) && actualIncomeByMonth.containsKey(monthKey);
            double income;
            double expense;
            if (useActual) {
                income = actualIncomeByMonth.getOrDefault(monthKey, 0.0);
                expense = actualExpenseByMonth.getOrDefault(monthKey, 0.0);
                actualMonthCount++;
            } else {
                List<Double> priorIncome = ForecastProjection.priorValues(incomeSeries, ym);
                List<Double> priorExpense = ForecastProjection.priorValues(expenseSeries, ym);
                income = ForecastProjection.projectMonth(priorIncome, m, incomeSeries, factor);
                expense = ForecastProjection.projectMonth(priorExpense, m, expenseSeries, 2 - factor * 0.5);
                expense += billsForMonth(m);
                double[] adjusted = applyScenarioAdjustments(income, expense, m, adjustments);
                income = adjusted[0];
                expense = adjusted[1];
            }
            double net = income - expense;
            yearIncome += income;
            yearExpense += expense;
            if (net < 0) {
                deficitMonths.add(monthKey);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("yearMonth", monthKey);
            row.put("income", round(income));
            row.put("expense", round(expense));
            row.put("net", round(net));
            if (!useActual) {
                putBounds(row, "income", income, spread);
                putBounds(row, "expense", expense, spread);
                putBounds(row, "net", net, spread);
            }
            row.put("budgetTarget", round(monthlyBudgetTarget));
            row.put("deficit", net < 0);
            row.put("actual", useActual);
            row.put("forecast", !useActual);
            months.add(row);
        }

        String runId = persist ? UUID.randomUUID().toString() : PREVIEW_RUN_PREFIX + year + "-" + scenario;
        List<Map<String, Object>> categoryForecasts = buildCategoryForecasts(
                year, factor, spread, userId, start, end, yearExpense);
        if (persist) {
            jdbcTemplate.update("insert into fin_forecast_run (id, user_id, scenario, target_year, params_json, created_at) "
                            + "values (?, ?, ?, ?, ?, now(3))",
                    runId, userId, scenario, year, "{\"method\":\"hybrid_projection\"}");
            persistForecastLines(runId, months, categoryForecasts, spread);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", year);
        out.put("scenario", scenario);
        out.put("runId", runId);
        out.put("preview", !persist);
        out.put("yearIncome", round(yearIncome));
        out.put("yearExpense", round(yearExpense));
        out.put("yearNet", round(yearIncome - yearExpense));
        putBounds(out, "yearIncome", yearIncome, spread);
        putBounds(out, "yearExpense", yearExpense, spread);
        putBounds(out, "yearNet", yearIncome - yearExpense, spread);
        out.put("deficitMonths", deficitMonths);
        out.put("months", months);
        out.put("categoryForecasts", categoryForecasts);
        out.put("confidence", Map.of(
                "halfWidthPct", spread.halfWidthPct(),
                "method", "hybrid_projection",
                "confidenceLevel", projectionQuality.confidenceLevel(),
                "sampleMonths", incomeSeries.size(),
                "errorBandSource", "scenario_spread"));
        out.put("confidenceLevel", projectionQuality.confidenceLevel());
        out.put("sampleMonths", incomeSeries.size());
        out.put("dataQualityImpact", gateMismatch ? "metrics_degraded" : "ok");
        out.put("budgetTarget", buildBudgetTarget(monthlyBudgetTarget, adjustments));
        out.put("explanation", buildExplanation(adjustments, actualMonthCount));
        out.put("budgetSuggestion", buildBudgetSuggestion(yearExpense, scenario, deficitMonths.size()));
        out.put("metricsGate", metricsGate);
        out.put("metricsSource", metricsSource);
        return out;
    }

    public Map<String, Object> categoryForecasts(int year, String scenario) throws Exception {
        Map<String, Object> forecast = forecastPreview(year, scenario, ForecastScenarioParams.empty());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("year", forecast.get("year"));
        out.put("scenario", forecast.get("scenario"));
        out.put("runId", forecast.get("runId"));
        out.put("confidence", forecast.get("confidence"));
        out.put("categories", forecast.get("categoryForecasts"));
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
        if (!params.containsKey("scenario")) {
            if (params.get("incomeChangePct") instanceof Number pct
                    && ((Number) params.get("incomeChangePct")).doubleValue() < -5) {
                scenario = "stress";
            } else if (params.get("newMonthlyBill") instanceof Number bill
                    && ((Number) params.get("newMonthlyBill")).doubleValue() > 0) {
                scenario = "conservative";
            }
        }
        ForecastScenarioParams adjustments = ForecastScenarioParams.fromMap(params);
        Map<String, Object> out = computeForecast(year, scenario, adjustments, true);
        out.put("inputParams", params);
        out.put("adjustments", adjustments.toMap());
        return out;
    }

    private static double[] applyScenarioAdjustments(double income,
                                                       double expense,
                                                       int month,
                                                       ForecastScenarioParams adjustments) {
        double incomeAdj = income;
        double expenseAdj = expense;
        if (adjustments.incomeChangePct() != null && adjustments.incomeChangePct() != 0) {
            incomeAdj *= 1 + adjustments.incomeChangePct() / 100.0;
        }
        if (adjustments.newMonthlyBill() != null && adjustments.newMonthlyBill() > 0) {
            expenseAdj += adjustments.newMonthlyBill();
        }
        if (adjustments.lumpSumExpense() != null && adjustments.lumpSumExpense() > 0 && month == 1) {
            expenseAdj += adjustments.lumpSumExpense();
        }
        return new double[] {incomeAdj, expenseAdj};
    }

    private double resolveMonthlyBudgetTarget(ForecastScenarioParams adjustments) {
        if (adjustments.targetMonthlyPayment() != null && adjustments.targetMonthlyPayment() > 0) {
            return adjustments.targetMonthlyPayment();
        }
        return budgetService.linesForBudget(budgetService.currentMonthlyBudget().getId()).stream()
                .map(BudgetLine::getLimitAmount)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    private static Map<String, Object> buildBudgetTarget(double monthlyCap, ForecastScenarioParams adjustments) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("monthlyCap", round(monthlyCap));
        target.put("annualCap", round(monthlyCap * 12));
        target.put("source", adjustments.targetMonthlyPayment() != null && adjustments.targetMonthlyPayment() > 0
                ? "targetMonthlyPayment"
                : "budget_lines");
        return target;
    }

    private static List<String> buildExplanation(ForecastScenarioParams adjustments, int actualMonthCount) {
        List<String> explanation = new ArrayList<>();
        explanation.add("Rolling 6-month average with seasonal index over the last 24 months of history.");
        explanation.addAll(adjustments.explanationLines());
        if (actualMonthCount > 0) {
            explanation.add(actualMonthCount + " completed month(s) use observed actuals; remainder are projections.");
        }
        return explanation;
    }

    private static Map<String, Double> monthlyMetricMapPreferring(List<Map<String, Object>> history,
                                                                 String primary,
                                                                 String fallback) {
        Map<String, Double> primaryMap = monthlyMetricMap(history, primary);
        if (!primaryMap.isEmpty()) {
            return primaryMap;
        }
        return monthlyMetricMap(history, fallback);
    }

    private static Map<String, Double> monthlyMetricMap(List<Map<String, Object>> history, String code) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map<String, Object> row : history) {
            if (code.equals(String.valueOf(row.get("metricCode")))) {
                out.put(metricMonthKey(row), ((Number) row.get("metricValue")).doubleValue());
            }
        }
        return out;
    }

    private static String metricMonthKey(Map<String, Object> row) {
        Object ym = row.get("yearMonth");
        if (ym != null && !String.valueOf(ym).isBlank() && !"null".equals(String.valueOf(ym))) {
            return String.valueOf(ym);
        }
        return String.valueOf(row.get("monthKey"));
    }

    private static boolean isCompletedMonth(YearMonth ym) {
        return !ym.isAfter(YearMonth.now().minusMonths(1));
    }

    private double billsForMonth(int month) {
        return billService.listEnabled().stream()
                .filter(b -> ForecastBillSupport.appliesInMonth(b, month))
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

    private List<Map<String, Object>> buildCategoryForecasts(int year,
                                                             double factor,
                                                             ForecastConfidence.Spread spread,
                                                             String userId,
                                                             YearMonth historyStart,
                                                             YearMonth historyEnd,
                                                             double yearExpenseTotal) {
        Map<String, CategoryHistory> histories = loadCategoryHistory(userId, historyStart, historyEnd);
        List<CategoryHistory> ranked = histories.values().stream()
                .sorted((a, b) -> Double.compare(b.total(), a.total()))
                .limit(TOP_CATEGORY_FORECASTS)
                .toList();

        List<Map<String, Object>> forecasts = new ArrayList<>();
        for (CategoryHistory history : ranked) {
            double catYearTotal = 0;
            List<Map<String, Object>> months = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                YearMonth ym = YearMonth.of(year, m);
                double seasonal = seasonalIndex(m);
                double amount = history.monthlyAvg() * seasonal * (2 - factor * 0.5);
                catYearTotal += amount;
                Map<String, Object> month = new LinkedHashMap<>();
                month.put("yearMonth", ym.format(YM));
                month.put("amount", round(amount));
                putBounds(month, "amount", amount, spread);
                months.add(month);
            }
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("categoryCode", history.code());
            cat.put("categoryName", history.name());
            cat.put("yearTotal", round(catYearTotal));
            putBounds(cat, "yearTotal", catYearTotal, spread);
            cat.put("sharePct", yearExpenseTotal > 0 ? round(catYearTotal / yearExpenseTotal * 100) : 0);
            cat.put("months", months);
            forecasts.add(cat);
        }
        return forecasts;
    }

    private Map<String, CategoryHistory> loadCategoryHistory(String userId, YearMonth start, YearMonth end) {
        LocalDate rangeStart = start.atDay(1);
        LocalDate rangeEndExclusive = end.plusMonths(1).atDay(1);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select v.category_code, v.category_name, v.txn_date, v.amount "
                        + "from v_transaction_analytics v "
                        + "inner join transaction t on t.id = v.id "
                        + "where v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 "
                        + "and v.amount > 0 and v.category_code is not null and v.category_code != '' "
                        + "and v.category_code != '__UNCLASSIFIED__' "
                        + "and v.txn_date >= ? and v.txn_date < ? "
                        + "and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))",
                rangeStart, rangeEndExclusive, userId, userId);

        Map<String, CategoryHistory> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String code = String.valueOf(row.get("category_code"));
            String name = String.valueOf(row.get("category_name"));
            YearMonth ym = YearMonth.from(AnalyticsDateRange.toLocalDate(row.get("txn_date")));
            double amount = ((Number) row.get("amount")).doubleValue();
            out.computeIfAbsent(code, k -> new CategoryHistory(code, name))
                    .addMonthAmount(ym, amount);
        }
        return out;
    }

    private static void putBounds(Map<String, Object> target, String prefix, double value, ForecastConfidence.Spread spread) {
        target.put(prefix + "Lower", ForecastConfidence.lower(value, spread));
        target.put(prefix + "Upper", ForecastConfidence.upper(value, spread));
    }

    private void persistForecastLines(String runId,
                                      List<Map<String, Object>> months,
                                      List<Map<String, Object>> categoryForecasts,
                                      ForecastConfidence.Spread spread) {
        if (!tableExists("fin_forecast_line")) {
            return;
        }
        for (Map<String, Object> month : months) {
            String monthKey = String.valueOf(month.get("yearMonth"));
            insertForecastLine(runId, monthKey, METRIC_INCOME_FORECAST,
                    ((Number) month.get("income")).doubleValue(), spread);
            insertForecastLine(runId, monthKey, METRIC_EXPENSE_FORECAST,
                    ((Number) month.get("expense")).doubleValue(), spread);
            insertForecastLine(runId, monthKey, METRIC_NET_FORECAST,
                    ((Number) month.get("net")).doubleValue(), spread);
        }
        for (Map<String, Object> category : categoryForecasts) {
            String metricCode = METRIC_CATEGORY_EXPENSE_PREFIX + category.get("categoryCode");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> catMonths = (List<Map<String, Object>>) category.get("months");
            for (Map<String, Object> month : catMonths) {
                insertForecastLine(runId, String.valueOf(month.get("yearMonth")), metricCode,
                        ((Number) month.get("amount")).doubleValue(), spread);
            }
        }
    }

    private void insertForecastLine(String runId, String monthKey, String metricCode, double value,
                                    ForecastConfidence.Spread spread) {
        BigDecimal metricValue = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
        BigDecimal lower = metricValue.multiply(BigDecimal.valueOf(spread.lowerFactor()))
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal upper = metricValue.multiply(BigDecimal.valueOf(spread.upperFactor()))
                .setScale(4, RoundingMode.HALF_UP);
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

    private record CategoryHistory(String code, String name, Map<YearMonth, Double> byMonth) {
        CategoryHistory(String code, String name) {
            this(code, name, new LinkedHashMap<>());
        }

        void addMonthAmount(YearMonth ym, double amount) {
            byMonth.merge(ym, amount, Double::sum);
        }

        double total() {
            return byMonth.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        double monthlyAvg() {
            if (byMonth.isEmpty()) {
                return 0;
            }
            List<YearMonth> sorted = byMonth.keySet().stream().sorted().toList();
            int window = Math.min(6, sorted.size());
            List<YearMonth> last = sorted.subList(sorted.size() - window, sorted.size());
            return last.stream().mapToDouble(ym -> byMonth.getOrDefault(ym, 0.0)).average().orElse(0);
        }
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
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
