package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.CashflowService;
import com.finsight.application.finance.DataQualityService;
import com.finsight.application.finance.WealthService;
import com.finsight.common.exception.AppServiceException;
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
import java.util.UUID;

@Service
public class FinancialProfileService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MetricMonthlyRepository metricRepository;
    private final WealthService wealthService;
    private final CashflowService cashflowService;
    private final DataQualityService dataQualityService;
    private final AuthenticationFacade authenticationFacade;
    private final JdbcTemplate jdbcTemplate;
    private final MetricGateService metricGateService;
    private final MetricMonthlyService metricMonthlyService;

    public FinancialProfileService(MetricMonthlyRepository metricRepository,
                                   WealthService wealthService,
                                   CashflowService cashflowService,
                                   DataQualityService dataQualityService,
                                   AuthenticationFacade authenticationFacade,
                                   JdbcTemplate jdbcTemplate,
                                   MetricGateService metricGateService,
                                   MetricMonthlyService metricMonthlyService) {
        this.metricRepository = metricRepository;
        this.wealthService = wealthService;
        this.cashflowService = cashflowService;
        this.dataQualityService = dataQualityService;
        this.authenticationFacade = authenticationFacade;
        this.jdbcTemplate = jdbcTemplate;
        this.metricGateService = metricGateService;
        this.metricMonthlyService = metricMonthlyService;
    }

    public Map<String, Object> currentProfile() throws Exception {
        String userId = userKey();
        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(11);
        Map<String, Object> metricsGate = metricGateService.status(3);
        boolean reportFallback = metricGateService.useReportFallback();
        List<Map<String, Object>> metrics = reportFallback
                ? metricMonthlyService.historyFromReports(start.format(YM), end.format(YM))
                : metricRepository.listForUser(userId, start.format(YM), end.format(YM));

        Map<String, Object> wealth = wealthService.snapshot();
        Map<String, Object> cashflow = cashflowService.metrics();
        Map<String, Object> quality = dataQualityService.summary();

        List<Map<String, Object>> dimensions = new ArrayList<>();
        dimensions.add(dimension("income_stability", scoreIncomeStability(metrics), "Income consistency over 12 months",
                List.of(ev("metric", "INCOME_TOTAL", metrics))));
        dimensions.add(dimension("spending_control", scoreSpendingControl(metrics), "Expense vs income balance",
                List.of(ev("metric", "EXPENSE_TOTAL", metrics))));
        dimensions.add(dimension("savings_discipline", scoreFromRate(wealth, "savingsRate", 0.2),
                "Year-to-date savings rate", List.of(ev("wealth", "savingsRate", wealth))));
        dimensions.add(dimension("fixed_burden", invertScore(wealth, "healthScore.fixedBurden", 35),
                "Fixed costs as % of income", List.of(ev("wealth", "fixedBurden", wealth))));
        dimensions.add(dimension("liquidity_safety", scoreRunway(cashflow), "Emergency runway in months",
                List.of(ev("cashflow", "runwayMonths", cashflow))));
        dimensions.add(dimension("debt_pressure", scoreDebt(wealth), "Debt payments vs income",
                List.of(ev("wealth", "debtPressure", wealth))));
        dimensions.add(dimension("lifestyle_inflation", scoreLifestyle(metrics), "Expense growth trend",
                List.of(ev("metric", "EXPENSE_TOTAL", metrics))));
        dimensions.add(dimension("spending_concentration", scoreConcentration(metrics),
                "Top-category concentration", List.of(ev("report", "category_breakdown", Map.of()))));
        dimensions.add(dimension("seasonality_risk", scoreSeasonality(metrics), "Month-to-month volatility",
                List.of(ev("metric", "NET_CASHFLOW", metrics))));
        dimensions.add(dimension("data_trust", scoreDataTrust(quality), "Classification completeness",
                List.of(ev("quality", "unclassifiedCount", quality))));

        double overall = dimensions.stream()
                .mapToDouble(d -> ((Number) d.get("score")).doubleValue())
                .average().orElse(0);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("overallScore", round(overall));
        out.put("userType", classifyUserType(dimensions));
        out.put("dimensions", dimensions);
        out.put("asOf", LocalDate.now().toString());
        out.put("metricsGate", metricsGate);
        out.put("metricsSource", reportFallback ? "report_sql" : "fin_metric_monthly");

        persistSnapshot(userId, dimensions);
        return out;
    }

    public List<Map<String, Object>> history(String from, String to) {
        return jdbcTemplate.queryForList(
                "select snapshot_date as snapshotDate, dimension, score, level_label as level, payload_json as payload "
                        + "from fin_profile_snapshot where user_id = ? and snapshot_date between ? and ? order by snapshot_date",
                userKey(), from, to);
    }

    private void persistSnapshot(String userId, List<Map<String, Object>> dimensions) {
        LocalDate today = LocalDate.now();
        for (Map<String, Object> dim : dimensions) {
            jdbcTemplate.update(
                    "insert into fin_profile_snapshot (id, user_id, snapshot_date, dimension, score, level_label, payload_json, created_at) "
                            + "values (?, ?, ?, ?, ?, ?, ?, now(3))",
                    UUID.randomUUID().toString(), userId, today, dim.get("id"), dim.get("score"),
                    dim.get("level"), com.alibaba.fastjson.JSON.toJSONString(dim));
        }
    }

    private static Map<String, Object> dimension(String id, double score, String summary, List<Map<String, Object>> evidence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("score", round(score));
        m.put("level", levelLabel(score));
        m.put("summary", summary);
        m.put("evidence", evidence);
        m.put("actions", defaultActions(id));
        return m;
    }

    private static List<Map<String, Object>> defaultActions(String id) {
        return switch (id) {
            case "data_trust" -> List.of(action("Review transactions", "open_transactions", "/transactions?unclassified=1"));
            case "spending_control", "lifestyle_inflation" -> List.of(action("Adjust budget", "adjust_budget", "/planning"));
            case "liquidity_safety", "savings_discipline" -> List.of(action("Set a goal", "open_goals", "/goals"));
            default -> List.of(action("View cashflow", "open_forecast", "/reports/cashflow"));
        };
    }

    private static Map<String, Object> action(String label, String type, String path) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("label", label);
        a.put("type", type);
        a.put("payload", Map.of("path", path));
        return a;
    }

    private static Map<String, Object> ev(String source, String ref, Object value) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("source", source);
        e.put("ref", ref);
        e.put("value", value);
        return e;
    }

    private static double scoreIncomeStability(List<Map<String, Object>> metrics) {
        List<Double> incomes = metricValues(metrics, "INCOME_TOTAL");
        if (incomes.size() < 2) {
            return 50;
        }
        double avg = incomes.stream().mapToDouble(d -> d).average().orElse(0);
        if (avg == 0) {
            return 40;
        }
        double cv = stdDev(incomes) / avg;
        return clamp(100 - cv * 100);
    }

    private static double scoreSpendingControl(List<Map<String, Object>> metrics) {
        double income = sumMetric(metrics, "INCOME_TOTAL");
        double expense = sumMetric(metrics, "EXPENSE_TOTAL");
        if (income <= 0) {
            return expense > 0 ? 30 : 60;
        }
        double rate = expense / income;
        return clamp(100 - rate * 80);
    }

    private static double scoreFromRate(Map<String, Object> wealth, String key, double target) {
        double rate = ((Number) wealth.getOrDefault(key, 0)).doubleValue();
        return clamp(rate / target * 80);
    }

    @SuppressWarnings("unchecked")
    private static double invertScore(Map<String, Object> wealth, String path, double threshold) {
        Map<String, Object> health = (Map<String, Object>) wealth.getOrDefault("healthScore", Map.of());
        double burden = ((Number) health.getOrDefault("fixedBurden", 0)).doubleValue();
        if (burden <= threshold) {
            return 85;
        }
        return clamp(100 - (burden - threshold) * 2);
    }

    private static double scoreRunway(Map<String, Object> cashflow) {
        double months = ((Number) cashflow.getOrDefault("runwayMonths", 0)).doubleValue();
        return clamp(months / 6 * 100);
    }

    @SuppressWarnings("unchecked")
    private static double scoreDebt(Map<String, Object> wealth) {
        Map<String, Object> health = (Map<String, Object>) wealth.getOrDefault("healthScore", Map.of());
        double pressure = ((Number) health.getOrDefault("debtPressure", 0)).doubleValue();
        return clamp(100 - pressure * 2);
    }

    private static double scoreLifestyle(List<Map<String, Object>> metrics) {
        List<Double> expenses = metricValues(metrics, "EXPENSE_TOTAL");
        if (expenses.size() < 3) {
            return 60;
        }
        double first = average(expenses.subList(0, expenses.size() / 2));
        double second = average(expenses.subList(expenses.size() / 2, expenses.size()));
        if (first <= 0) {
            return 60;
        }
        double growth = (second - first) / first;
        return clamp(100 - growth * 120);
    }

    private static double scoreConcentration(List<Map<String, Object>> metrics) {
        return metrics.isEmpty() ? 55 : 70;
    }

    private static double scoreSeasonality(List<Map<String, Object>> metrics) {
        List<Double> nets = metricValues(metrics, "NET_CASHFLOW");
        if (nets.size() < 2) {
            return 55;
        }
        double avg = average(nets);
        if (avg == 0) {
            return 50;
        }
        return clamp(100 - (stdDev(nets) / Math.abs(avg)) * 50);
    }

    private static double scoreDataTrust(Map<String, Object> quality) {
        int uncls = ((Number) quality.getOrDefault("unclassifiedCount", 0)).intValue();
        return clamp(100 - Math.min(90, uncls / 5.0));
    }

    private static String classifyUserType(List<Map<String, Object>> dimensions) {
        double savings = scoreOf(dimensions, "savings_discipline");
        double fixed = scoreOf(dimensions, "fixed_burden");
        if (savings >= 75 && fixed < 50) {
            return "high_savings_high_fixed";
        }
        if (savings >= 75) {
            return "disciplined_saver";
        }
        if (fixed < 45) {
            return "high_fixed_burden";
        }
        return "balanced";
    }

    private static double scoreOf(List<Map<String, Object>> dimensions, String id) {
        return dimensions.stream()
                .filter(d -> id.equals(d.get("id")))
                .mapToDouble(d -> ((Number) d.get("score")).doubleValue())
                .findFirst().orElse(50);
    }

    private static List<Double> metricValues(List<Map<String, Object>> metrics, String code) {
        List<Double> out = new ArrayList<>();
        for (Map<String, Object> row : metrics) {
            if (code.equals(String.valueOf(row.get("metricCode")))) {
                out.add(((Number) row.get("metricValue")).doubleValue());
            }
        }
        return out;
    }

    private static double sumMetric(List<Map<String, Object>> metrics, String code) {
        return metricValues(metrics, code).stream().mapToDouble(d -> d).sum();
    }

    private static double average(List<Double> values) {
        return values.stream().mapToDouble(d -> d).average().orElse(0);
    }

    private static double stdDev(List<Double> values) {
        double avg = average(values);
        double var = values.stream().mapToDouble(v -> (v - avg) * (v - avg)).average().orElse(0);
        return Math.sqrt(var);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static String levelLabel(double score) {
        if (score >= 75) {
            return "strong";
        }
        if (score >= 50) {
            return "moderate";
        }
        return "needs_attention";
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
