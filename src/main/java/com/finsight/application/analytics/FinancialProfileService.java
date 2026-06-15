package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.CashflowService;
import com.finsight.application.finance.DataQualityService;
import com.finsight.application.finance.WealthService;
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
                incomeStabilityEvidence(metrics)));
        dimensions.add(dimension("spending_control", scoreSpendingControl(metrics), "Expense vs income balance",
                spendingControlEvidence(metrics)));
        dimensions.add(dimension("savings_discipline", scoreFromRate(wealth, "savingsRate", 0.2),
                "Year-to-date savings rate", savingsDisciplineEvidence(wealth)));
        dimensions.add(dimension("fixed_burden", invertScore(wealth, "healthScore.fixedBurden", 35),
                "Fixed costs as % of income", fixedBurdenEvidence(wealth)));
        dimensions.add(dimension("liquidity_safety", scoreRunway(cashflow), "Emergency runway in months",
                liquidityEvidence(cashflow)));
        dimensions.add(dimension("debt_pressure", scoreDebt(wealth), "Debt payments vs income",
                debtPressureEvidence(wealth)));
        dimensions.add(dimension("lifestyle_inflation", scoreLifestyle(metrics), "Expense growth trend",
                lifestyleEvidence(metrics)));
        dimensions.add(dimension("spending_concentration", scoreConcentration(metrics),
                "Top-category concentration", concentrationEvidence(metrics)));
        dimensions.add(dimension("seasonality_risk", scoreSeasonality(metrics), "Month-to-month volatility",
                seasonalityEvidence(metrics)));
        dimensions.add(dimension("data_trust", scoreDataTrust(quality), "Classification completeness",
                dataTrustEvidence(quality)));

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

    public List<Map<String, Object>> history(String from, String to, String dimension) {
        if (dimension != null && !dimension.isBlank()) {
            return jdbcTemplate.queryForList(
                    "select snapshot_date as snapshotDate, dimension, score, level_label as level, payload_json as payload "
                            + "from fin_profile_snapshot where user_id = ? and snapshot_date between ? and ? "
                            + "and dimension = ? order by snapshot_date",
                    userKey(), from, to, dimension);
        }
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
            case "data_trust" -> List.of(
                    action("Review unclassified transactions", "open_transactions", "/transactions?unclassified=1"),
                    action("Tune classification rules", "open_rules", "/admin/rules"));
            case "income_stability" -> List.of(
                    action("View income curve", "open_report", "/reports/income-curve"),
                    action("Open income ledger", "open_ledger", "/ledgers/salary"));
            case "spending_control" -> List.of(
                    action("Budget vs actual", "open_report", "/reports/budget-vs-actual"),
                    action("Income vs expense", "open_report", "/reports/income-vs-expense"));
            case "savings_discipline" -> List.of(
                    action("Set a savings goal", "open_goals", "/goals"),
                    action("Wealth overview", "open_wealth", "/wealth"));
            case "fixed_burden" -> List.of(
                    action("Fixed vs variable costs", "open_report", "/reports/fixed-vs-variable"),
                    action("Open planning", "open_planning", "/planning"));
            case "liquidity_safety" -> List.of(
                    action("Cash risk calendar", "open_report", "/reports/cash-risk"),
                    action("Cashflow report", "open_report", "/reports/cashflow"));
            case "debt_pressure" -> List.of(
                    action("Wealth & debt view", "open_wealth", "/wealth"),
                    action("Expense ledger", "open_ledger", "/ledgers/expense"));
            case "lifestyle_inflation" -> List.of(
                    action("Spending drift", "open_report", "/reports/spending-drift"),
                    action("Adjust budget", "open_planning", "/planning"));
            case "spending_concentration" -> List.of(
                    action("Category breakdown", "open_report", "/reports/category-breakdown"),
                    action("Category comparison", "open_report", "/reports/category-comparison"));
            case "seasonality_risk" -> List.of(
                    action("Trend changes", "open_report", "/reports/trend-changes"),
                    action("Monthly comparison", "open_report", "/reports/monthly-comparison"));
            default -> List.of(action("View cashflow", "open_report", "/reports/cashflow"));
        };
    }

    private static Map<String, Object> action(String label, String type, String path) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("label", label);
        a.put("type", type);
        a.put("payload", Map.of("path", path));
        return a;
    }

    private static Map<String, Object> ev(String source, String ref, String label, String detail, Object value) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("source", source);
        e.put("ref", ref);
        e.put("label", label);
        e.put("detail", detail);
        e.put("value", value);
        return e;
    }

    private static List<Map<String, Object>> incomeStabilityEvidence(List<Map<String, Object>> metrics) {
        List<Double> incomes = metricValues(metrics, "INCOME_TOTAL");
        if (incomes.isEmpty()) {
            return List.of(ev("metric", "INCOME_TOTAL", "Income history", "Not enough months to measure stability", "—"));
        }
        double avg = average(incomes);
        double cv = avg == 0 ? 0 : stdDev(incomes) / avg;
        return List.of(ev("metric", "INCOME_TOTAL", "12-month income variability",
                "Coefficient of variation — lower is more stable",
                formatPct(cv * 100) + " · avg " + formatMoney(avg) + "/mo · " + incomes.size() + " months"));
    }

    private static List<Map<String, Object>> spendingControlEvidence(List<Map<String, Object>> metrics) {
        double income = sumMetric(metrics, "INCOME_TOTAL");
        double expense = sumMetric(metrics, "EXPENSE_TOTAL");
        double rate = income > 0 ? expense / income : 0;
        return List.of(ev("metric", "EXPENSE_RATIO", "Expense to income (12 mo)",
                "Spending above income reduces this score",
                formatPct(rate * 100) + " · income " + formatMoney(income) + " · expense " + formatMoney(expense)));
    }

    private static List<Map<String, Object>> savingsDisciplineEvidence(Map<String, Object> wealth) {
        double rate = ((Number) wealth.getOrDefault("savingsRate", 0)).doubleValue();
        return List.of(ev("wealth", "savingsRate", "Year-to-date savings rate",
                "Target reference: 20%+",
                formatPct(rate * 100)));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fixedBurdenEvidence(Map<String, Object> wealth) {
        Map<String, Object> health = (Map<String, Object>) wealth.getOrDefault("healthScore", Map.of());
        double burden = ((Number) health.getOrDefault("fixedBurden", 0)).doubleValue();
        return List.of(ev("wealth", "fixedBurden", "Fixed costs share of income",
                "Above 35% is considered high fixed burden",
                formatPct(burden)));
    }

    private static List<Map<String, Object>> liquidityEvidence(Map<String, Object> cashflow) {
        double months = ((Number) cashflow.getOrDefault("runwayMonths", 0)).doubleValue();
        return List.of(ev("cashflow", "runwayMonths", "Emergency runway",
                "Months of expenses covered by liquid balance",
                round(months) + " months"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> debtPressureEvidence(Map<String, Object> wealth) {
        Map<String, Object> health = (Map<String, Object>) wealth.getOrDefault("healthScore", Map.of());
        double pressure = ((Number) health.getOrDefault("debtPressure", 0)).doubleValue();
        return List.of(ev("wealth", "debtPressure", "Debt service pressure",
                "Higher debt payments relative to income lower this score",
                formatPct(pressure)));
    }

    private static List<Map<String, Object>> lifestyleEvidence(List<Map<String, Object>> metrics) {
        List<Double> expenses = metricValues(metrics, "EXPENSE_TOTAL");
        if (expenses.size() < 3) {
            return List.of(ev("metric", "EXPENSE_TOTAL", "Expense trend", "Need more months to detect lifestyle drift", "—"));
        }
        double first = average(expenses.subList(0, expenses.size() / 2));
        double second = average(expenses.subList(expenses.size() / 2, expenses.size()));
        double growth = first > 0 ? (second - first) / first : 0;
        return List.of(ev("metric", "EXPENSE_GROWTH", "Recent vs earlier spending",
                "Compares average expense in recent half vs earlier half of the window",
                formatSignedPct(growth * 100) + " · recent " + formatMoney(second) + "/mo"));
    }

    private static List<Map<String, Object>> concentrationEvidence(List<Map<String, Object>> metrics) {
        return List.of(ev("report", "category_breakdown", "Category concentration",
                "Open category breakdown to inspect top spend buckets",
                metrics.isEmpty() ? "Limited metric history" : "See category breakdown report"));
    }

    private static List<Map<String, Object>> seasonalityEvidence(List<Map<String, Object>> metrics) {
        List<Double> nets = metricValues(metrics, "NET_CASHFLOW");
        if (nets.size() < 2) {
            return List.of(ev("metric", "NET_CASHFLOW", "Net cashflow volatility", "Not enough months", "—"));
        }
        double avg = average(nets);
        double vol = avg == 0 ? stdDev(nets) : stdDev(nets) / Math.abs(avg);
        return List.of(ev("metric", "NET_CASHFLOW", "Net cashflow swing",
                "Higher month-to-month swings increase seasonality risk",
                "volatility index " + round(vol * 100) + " · avg net " + formatMoney(avg) + "/mo"));
    }

    private static List<Map<String, Object>> dataTrustEvidence(Map<String, Object> quality) {
        int uncls = ((Number) quality.getOrDefault("unclassifiedCount", 0)).intValue();
        int total = ((Number) quality.getOrDefault("totalCount", 0)).intValue();
        return List.of(ev("quality", "unclassifiedCount", "Unclassified transactions",
                "Classify or rule-tag rows to improve profile accuracy",
                uncls + " unclassified" + (total > 0 ? " of " + total + " rows" : "")));
    }

    private static String formatPct(double pct) {
        return round(pct) + "%";
    }

    private static String formatSignedPct(double pct) {
        return (pct >= 0 ? "+" : "") + round(pct) + "%";
    }

    private static String formatMoney(double amount) {
        return "¥" + BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP).toPlainString();
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
