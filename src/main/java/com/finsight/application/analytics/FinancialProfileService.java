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
    private static final double SAVINGS_TARGET = 0.2;
    private static final double FIXED_BURDEN_THRESHOLD = 35;
    private static final double LIQUIDITY_TARGET_MONTHS = 6;

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
        ProfileScoring.ConcentrationStats concentration = loadConcentrationStats(userId, start, end);

        List<Double> incomes = metricValues(metrics, "INCOME_TOTAL");
        List<Double> expenses = metricValues(metrics, "EXPENSE_TOTAL");
        List<Double> nets = metricValues(metrics, "NET_CASHFLOW");
        double incomeTotal = sumValues(incomes);
        double expenseTotal = sumValues(expenses);
        double savingsRate = ((Number) wealth.getOrDefault("savingsRate", 0)).doubleValue();
        Map<String, Object> health = healthScore(wealth);
        double fixedBurden = ((Number) health.getOrDefault("fixedBurden", 0)).doubleValue();
        double debtPressure = ((Number) health.getOrDefault("debtPressure", 0)).doubleValue();
        double runwayMonths = ((Number) cashflow.getOrDefault("runwayMonths", 0)).doubleValue();
        int unclassified = ((Number) quality.getOrDefault("unclassifiedCount", 0)).intValue();
        int totalRows = ((Number) quality.getOrDefault("totalCount", 0)).intValue();

        List<Map<String, Object>> dimensions = new ArrayList<>();

        double incomeScore = ProfileScoring.scoreIncomeStability(incomes);
        dimensions.add(dimension("income_stability", incomeScore,
                "Income consistency over 12 months",
                ProfileScoring.incomeStabilityReason(incomeScore, incomes),
                incomeStabilityEvidence(incomes)));

        double spendingScore = ProfileScoring.scoreSpendingControl(incomeTotal, expenseTotal);
        dimensions.add(dimension("spending_control", spendingScore,
                "Expense vs income balance",
                ProfileScoring.spendingControlReason(spendingScore, incomeTotal, expenseTotal),
                spendingControlEvidence(incomeTotal, expenseTotal)));

        double savingsScore = ProfileScoring.scoreSavingsDiscipline(savingsRate, SAVINGS_TARGET);
        dimensions.add(dimension("savings_discipline", savingsScore,
                "Year-to-date savings rate",
                ProfileScoring.savingsDisciplineReason(savingsScore, savingsRate, SAVINGS_TARGET),
                savingsDisciplineEvidence(savingsRate)));

        double fixedScore = ProfileScoring.scoreFixedBurden(fixedBurden, FIXED_BURDEN_THRESHOLD);
        dimensions.add(dimension("fixed_burden", fixedScore,
                "Fixed costs as % of income",
                ProfileScoring.fixedBurdenReason(fixedScore, fixedBurden, FIXED_BURDEN_THRESHOLD),
                fixedBurdenEvidence(fixedBurden)));

        double liquidityScore = ProfileScoring.scoreLiquidity(runwayMonths, LIQUIDITY_TARGET_MONTHS);
        dimensions.add(dimension("liquidity_safety", liquidityScore,
                "Emergency runway in months",
                ProfileScoring.liquidityReason(liquidityScore, runwayMonths, LIQUIDITY_TARGET_MONTHS),
                liquidityEvidence(runwayMonths)));

        double debtScore = ProfileScoring.scoreDebtPressure(debtPressure);
        dimensions.add(dimension("debt_pressure", debtScore,
                "Debt payments vs income",
                ProfileScoring.debtPressureReason(debtScore, debtPressure),
                debtPressureEvidence(debtPressure)));

        double lifestyleScore = ProfileScoring.scoreLifestyleInflation(expenses);
        dimensions.add(dimension("lifestyle_inflation", lifestyleScore,
                "Expense growth trend",
                ProfileScoring.lifestyleInflationReason(lifestyleScore, expenses),
                lifestyleEvidence(expenses)));

        double concentrationScore = ProfileScoring.scoreSpendingConcentration(concentration.topSharePct());
        dimensions.add(dimension("spending_concentration", concentrationScore,
                "Top-category concentration",
                ProfileScoring.spendingConcentrationReason(concentrationScore, concentration),
                concentrationEvidence(concentration)));

        double seasonalityScore = ProfileScoring.scoreSeasonality(nets);
        dimensions.add(dimension("seasonality_risk", seasonalityScore,
                "Month-to-month volatility",
                ProfileScoring.seasonalityReason(seasonalityScore, nets),
                seasonalityEvidence(nets)));

        double dataScore = ProfileScoring.scoreDataTrust(unclassified);
        dimensions.add(dimension("data_trust", dataScore,
                "Classification completeness",
                ProfileScoring.dataTrustReason(dataScore, unclassified, totalRows),
                dataTrustEvidence(unclassified, totalRows)));

        double overall = dimensions.stream()
                .mapToDouble(d -> ((Number) d.get("score")).doubleValue())
                .average().orElse(0);
        ProfileScoring.UserTypeResult userType = ProfileScoring.classifyUserType(
                ProfileScoring.scoresFromDimensions(dimensions));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("overallScore", ProfileScoring.round(overall));
        out.put("userType", userType.type());
        out.put("userTypeExplanation", userType.explanation());
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

    private ProfileScoring.ConcentrationStats loadConcentrationStats(String userId, YearMonth start, YearMonth end) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select v.category_code, v.category_name, sum(v.amount) as amount "
                        + "from v_transaction_analytics v "
                        + "inner join transaction t on t.id = v.id "
                        + "where v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 "
                        + "and v.amount > 0 and v.category_code is not null and v.category_code != '' "
                        + "and v.category_code != '__UNCLASSIFIED__' "
                        + "and date_format(v.txn_date, '%Y-%m') between ? and ? "
                        + "and (t.created_by = ? or (? = '_anonymous' and t.created_by is null)) "
                        + "group by v.category_code, v.category_name "
                        + "order by amount desc",
                start.format(YM), end.format(YM), userId, userId);
        return ProfileScoring.concentrationFromRows(rows);
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

    private static Map<String, Object> dimension(String id,
                                                 double score,
                                                 String summary,
                                                 String reason,
                                                 List<Map<String, Object>> evidence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("score", ProfileScoring.round(score));
        m.put("level", ProfileScoring.levelLabel(score));
        m.put("summary", summary);
        m.put("reason", reason);
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

    private static List<Map<String, Object>> incomeStabilityEvidence(List<Double> incomes) {
        if (incomes.isEmpty()) {
            return List.of(ev("metric", "INCOME_TOTAL", "Income history", "Not enough months to measure stability", "—"));
        }
        double avg = average(incomes);
        double cv = avg == 0 ? 0 : stdDev(incomes) / avg;
        return List.of(ev("metric", "INCOME_CV", "Income variability",
                "Lower coefficient of variation means steadier pay",
                formatPct(cv * 100) + " CV · avg " + formatMoney(avg) + "/mo"));
    }

    private static List<Map<String, Object>> spendingControlEvidence(double income, double expense) {
        double rate = income > 0 ? expense / income : 0;
        return List.of(ev("metric", "EXPENSE_RATIO", "Expense to income (12 mo)",
                "Spending above income reduces this score",
                formatPct(rate * 100) + " · " + formatMoney(expense) + " spent vs " + formatMoney(income) + " earned"));
    }

    private static List<Map<String, Object>> savingsDisciplineEvidence(double rate) {
        return List.of(ev("wealth", "savingsRate", "Year-to-date savings rate",
                "Reference target: 20%+",
                formatPct(rate * 100)));
    }

    private static List<Map<String, Object>> fixedBurdenEvidence(double burden) {
        return List.of(ev("wealth", "fixedBurden", "Fixed costs share of income",
                "Above 35% is considered high fixed burden",
                formatPct(burden)));
    }

    private static List<Map<String, Object>> liquidityEvidence(double months) {
        return List.of(ev("cashflow", "runwayMonths", "Emergency runway",
                "Target: 6 months of expenses in liquid balance",
                ProfileScoring.round(months) + " months"));
    }

    private static List<Map<String, Object>> debtPressureEvidence(double pressure) {
        return List.of(ev("wealth", "debtPressure", "Debt service pressure",
                "Higher debt payments relative to income lower this score",
                formatPct(pressure)));
    }

    private static List<Map<String, Object>> lifestyleEvidence(List<Double> expenses) {
        if (expenses.size() < 3) {
            return List.of(ev("metric", "EXPENSE_GROWTH", "Expense trend", "Need more months to detect lifestyle drift", "—"));
        }
        double first = average(expenses.subList(0, expenses.size() / 2));
        double second = average(expenses.subList(expenses.size() / 2, expenses.size()));
        double growth = first > 0 ? (second - first) / first : 0;
        return List.of(ev("metric", "EXPENSE_GROWTH", "Recent vs earlier spending",
                "Compares average expense in recent half vs earlier half of the window",
                formatSignedPct(growth * 100) + " · recent " + formatMoney(second) + "/mo"));
    }

    private static List<Map<String, Object>> concentrationEvidence(ProfileScoring.ConcentrationStats stats) {
        if (stats.totalExpense() <= 0) {
            return List.of(ev("report", "category_breakdown", "Category concentration",
                    "Classify expenses to measure concentration", "No categorized spend in window"));
        }
        return List.of(ev("report", stats.topCategoryCode(), "Top spend category",
                "Share of total categorized expense in the last 12 months",
                stats.topCategoryName() + " · " + formatPct(stats.topSharePct()) + " of spend"));
    }

    private static List<Map<String, Object>> seasonalityEvidence(List<Double> nets) {
        if (nets.size() < 2) {
            return List.of(ev("metric", "NET_CASHFLOW", "Net cashflow volatility", "Not enough months", "—"));
        }
        double avg = average(nets);
        double vol = avg == 0 ? stdDev(nets) : stdDev(nets) / Math.abs(avg);
        return List.of(ev("metric", "NET_VOLATILITY", "Net cashflow swing",
                "Higher month-to-month swings increase seasonality risk",
                "volatility index " + ProfileScoring.round(vol * 100) + " · avg net " + formatMoney(avg) + "/mo"));
    }

    private static List<Map<String, Object>> dataTrustEvidence(int uncls, int total) {
        return List.of(ev("quality", "unclassifiedCount", "Unclassified transactions",
                "Classify or rule-tag rows to improve profile accuracy",
                uncls + " unclassified" + (total > 0 ? " of " + total + " rows" : "")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> healthScore(Map<String, Object> wealth) {
        return (Map<String, Object>) wealth.getOrDefault("healthScore", Map.of());
    }

    private static String formatPct(double pct) {
        return ProfileScoring.round(pct) + "%";
    }

    private static String formatSignedPct(double pct) {
        return (pct >= 0 ? "+" : "") + ProfileScoring.round(pct) + "%";
    }

    private static String formatMoney(double amount) {
        return "¥" + BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP).toPlainString();
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

    private static double sumValues(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).sum();
    }

    private static double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static double stdDev(List<Double> values) {
        double avg = average(values);
        double var = values.stream().mapToDouble(v -> (v - avg) * (v - avg)).average().orElse(0);
        return Math.sqrt(var);
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
