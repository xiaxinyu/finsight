package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.classification.FinanceSemanticsCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrendAnalysisService {

    private final AuthenticationFacade authenticationFacade;
    private final FinanceSemanticMetricsRepository semanticMetricsRepository;
    private final JdbcTemplate jdbcTemplate;

    public TrendAnalysisService(AuthenticationFacade authenticationFacade,
                                FinanceSemanticMetricsRepository semanticMetricsRepository,
                                JdbcTemplate jdbcTemplate) {
        this.authenticationFacade = authenticationFacade;
        this.semanticMetricsRepository = semanticMetricsRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> trends(int fromYear, int toYear) throws Exception {
        return trends(fromYear, toYear, fromYear);
    }

    public Map<String, Object> trends(int fromYear, int toYear, int historyFromYear) throws Exception {
        String userId = userKey();
        LocalDate asOf = LocalDate.now();
        int matrixFrom = Math.min(historyFromYear, fromYear);
        boolean ytdCompare = toYear == asOf.getYear();

        double incomeFrom = consumptionOrIncomeTotal(true, fromYear, toYear, asOf, true);
        double incomeTo = consumptionOrIncomeTotal(true, toYear, toYear, asOf, false);
        double expenseFrom = consumptionOrIncomeTotal(false, fromYear, toYear, asOf, true);
        double expenseTo = consumptionOrIncomeTotal(false, toYear, toYear, asOf, false);
        double fixedFrom = fixedCostTotal(fromYear, toYear, asOf, true);
        double fixedTo = fixedCostTotal(toYear, toYear, asOf, false);

        double incomeDelta = incomeTo - incomeFrom;
        double expenseDelta = expenseTo - expenseFrom;
        double savingsFrom = incomeFrom > 0 ? (incomeFrom - expenseFrom) / incomeFrom * 100 : 0;
        double savingsTo = incomeTo > 0 ? (incomeTo - expenseTo) / incomeTo * 100 : 0;

        List<Map<String, Object>> categoryRows = loadSemanticCategoryRows(fromYear, toYear, userId, asOf);
        List<Map<String, Object>> matrixRows = loadSemanticCategoryRows(matrixFrom, toYear, userId, asOf);
        List<Map<String, Object>> l1MatrixRows = loadCategoryL1Rows(matrixFrom, toYear, userId, asOf);
        List<Map<String, Object>> consumptionYearSeries = buildConsumptionYearSeries(matrixFrom, toYear, asOf);
        List<Map<String, Object>> topCategoryGrowth = enrichCategoryMovers(categoryRows, fromYear, toYear, expenseDelta);

        Map<String, Double> merchantFrom = merchantSpendForYear(fromYear, userId, asOf, toYear, true);
        Map<String, Double> merchantTo = merchantSpendForYear(toYear, userId, asOf, toYear, false);
        Map<String, String> merchantLabels = merchantLabels(merchantFrom, merchantTo);
        List<Map<String, Object>> topMerchantMovers = enrichMerchantMovers(
                TrendDecomposition.topMovers(merchantFrom, merchantTo, merchantLabels, expenseDelta, 8),
                toYear);

        double incomePct = TrendDecomposition.pctChange(incomeFrom, incomeTo);
        double expensePct = TrendDecomposition.pctChange(expenseFrom, expenseTo);
        boolean lifestyleDetected = TrendDecomposition.lifestyleInflationDetected(incomePct, expensePct, expenseDelta);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("income", TrendDecomposition.deltaMetric(incomeFrom, incomeTo));
        summary.put("expense", TrendDecomposition.deltaMetric(expenseFrom, expenseTo));
        summary.put("savingsRate", savingsRateMetric(savingsFrom, savingsTo));
        summary.put("fixedCost", TrendDecomposition.deltaMetric(fixedFrom, fixedTo));
        summary.put("headline", buildHeadline(expenseDelta, topCategoryGrowth, topMerchantMovers));

        List<Map<String, Object>> trendItems = buildTrendItems(
                fromYear, toYear, summary, topCategoryGrowth, topMerchantMovers, lifestyleDetected, incomePct, expensePct);

        Map<String, Object> lifestyleInflation = new LinkedHashMap<>();
        lifestyleInflation.put("detected", lifestyleDetected);
        lifestyleInflation.put("incomePctChange", round(incomePct));
        lifestyleInflation.put("expensePctChange", round(expensePct));
        lifestyleInflation.put("gapPct", round(expensePct - incomePct));
        lifestyleInflation.put("note", lifestyleDetected
                ? "Spending grew faster than income — review discretionary categories and top merchants."
                : "Expense growth is in line with or below income growth.");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fromYear", fromYear);
        out.put("toYear", toYear);
        out.put("summary", summary);
        out.put("topCategoryGrowth", topCategoryGrowth);
        out.put("topMerchantMovers", topMerchantMovers);
        out.put("consumptionYearSeries", consumptionYearSeries);
        out.put("compareMode", ytdCompare ? "ytd_aligned" : "full_year");
        out.put("historyFromYear", matrixFrom);
        out.put("categoryYearMatrix", buildCategoryYearMatrix(matrixRows, matrixFrom, toYear, consumptionYearSeries, MatrixDrillMode.SEMANTIC_TAG));
        out.put("categoryL1YearMatrix", buildCategoryYearMatrix(l1MatrixRows, matrixFrom, toYear, consumptionYearSeries, MatrixDrillMode.CATEGORY_L1));
        out.put("savingsInflection", Map.of(
                "fromYear", fromYear,
                "toYear", toYear,
                "fromRate", round(savingsFrom),
                "toRate", round(savingsTo),
                "deltaPercent", round(savingsTo - savingsFrom)));
        out.put("lifestyleInflation", lifestyleInflation);
        out.put("trends", trendItems);
        out.put("metricsSource", "v_transaction_finance_semantics.semantic_tag");
        out.put("user", userId);
        return out;
    }

    private List<Map<String, Object>> buildTrendItems(int fromYear,
                                                     int toYear,
                                                     Map<String, Object> summary,
                                                     List<Map<String, Object>> categories,
                                                     List<Map<String, Object>> merchants,
                                                     boolean lifestyleDetected,
                                                     double incomePct,
                                                     double expensePct) {
        List<Map<String, Object>> items = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> income = (Map<String, Object>) summary.get("income");
        @SuppressWarnings("unchecked")
        Map<String, Object> expense = (Map<String, Object>) summary.get("expense");
        @SuppressWarnings("unchecked")
        Map<String, Object> savings = (Map<String, Object>) summary.get("savingsRate");
        @SuppressWarnings("unchecked")
        Map<String, Object> fixed = (Map<String, Object>) summary.get("fixedCost");

        items.add(TrendDecomposition.trendItem(
                "income_yoy",
                "Income change",
                ((Number) income.get("deltaAmount")).doubleValue(),
                ((Number) income.get("deltaPercent")).doubleValue(),
                0,
                drillYear(toYear, "income")));
        items.add(TrendDecomposition.trendItem(
                "expense_yoy",
                "Expense change",
                ((Number) expense.get("deltaAmount")).doubleValue(),
                ((Number) expense.get("deltaPercent")).doubleValue(),
                100,
                drillYear(toYear, "expense")));
        items.add(TrendDecomposition.trendItem(
                "savings_rate",
                "Savings rate change",
                ((Number) savings.get("deltaAmount")).doubleValue(),
                ((Number) savings.get("deltaPercent")).doubleValue(),
                0,
                drillYear(toYear, "expense")));
        items.add(TrendDecomposition.trendItem(
                "fixed_cost",
                "Fixed cost change",
                ((Number) fixed.get("deltaAmount")).doubleValue(),
                ((Number) fixed.get("deltaPercent")).doubleValue(),
                TrendDecomposition.contributionPct(
                        ((Number) fixed.get("deltaAmount")).doubleValue(),
                        ((Number) expense.get("deltaAmount")).doubleValue()),
                drillYear(toYear, "expense")));

        for (Map<String, Object> cat : categories) {
            items.add(TrendDecomposition.trendItem(
                    "category_mover",
                    String.valueOf(cat.get("categoryName")),
                    ((Number) cat.get("deltaAmount")).doubleValue(),
                    ((Number) cat.get("pctChange")).doubleValue(),
                    ((Number) cat.get("contributionPct")).doubleValue(),
                    drillSemanticTag(toYear, String.valueOf(cat.get("categoryCode")), String.valueOf(cat.get("categoryName")))));
        }
        for (Map<String, Object> merchant : merchants) {
            @SuppressWarnings("unchecked")
            Map<String, String> drill = (Map<String, String>) merchant.get("drillDown");
            items.add(TrendDecomposition.trendItem(
                    "merchant_mover",
                    String.valueOf(merchant.get("label")),
                    ((Number) merchant.get("deltaAmount")).doubleValue(),
                    ((Number) merchant.get("deltaPercent")).doubleValue(),
                    ((Number) merchant.get("contributionPct")).doubleValue(),
                    drill));
        }
        if (lifestyleDetected) {
            items.add(TrendDecomposition.trendItem(
                    "lifestyle_inflation",
                    "Lifestyle inflation",
                    ((Number) expense.get("deltaAmount")).doubleValue(),
                    expensePct - incomePct,
                    0,
                    drillYear(toYear, "expense")));
        }
        return items;
    }

    private List<Map<String, Object>> enrichCategoryMovers(List<Map<String, Object>> rows,
                                                           int fromYear,
                                                           int toYear,
                                                           double expenseDelta) {
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, Double> from = new LinkedHashMap<>();
        Map<String, Double> to = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            int year = ((Number) row.get("year")).intValue();
            String tagId = String.valueOf(row.get("categoryCode"));
            names.putIfAbsent(tagId, String.valueOf(row.get("categoryName")));
            double amt = ((Number) row.get("amount")).doubleValue();
            if (year == fromYear) {
                from.merge(tagId, amt, Double::sum);
            }
            if (year == toYear) {
                to.merge(tagId, amt, Double::sum);
            }
        }
        List<Map<String, Object>> movers = new ArrayList<>();
        for (String tagId : to.keySet()) {
            double start = from.getOrDefault(tagId, 0.0);
            double end = to.getOrDefault(tagId, 0.0);
            double delta = end - start;
            double pct = TrendDecomposition.pctChange(start, end);
            if (Math.abs(pct) < 10 && Math.abs(delta) < 100) {
                continue;
            }
            String label = names.getOrDefault(tagId, tagId);
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("categoryCode", tagId);
            g.put("categoryName", label);
            g.put("classification", label);
            g.put("txnType", FinanceSemanticsCatalog.semanticTagTxnTypeLabel(tagId));
            g.put("fromAmount", round(start));
            g.put("toAmount", round(end));
            g.put("pctChange", Math.round(pct));
            g.put("deltaAmount", round(delta));
            g.put("deltaPercent", round(pct));
            g.put("contributionPct", round(TrendDecomposition.contributionPct(delta, expenseDelta)));
            g.put("drillDown", drillSemanticTag(toYear, tagId, label));
            movers.add(g);
        }
        movers.sort((a, b) -> Double.compare(
                Math.abs(((Number) b.get("deltaAmount")).doubleValue()),
                Math.abs(((Number) a.get("deltaAmount")).doubleValue())));
        return movers.size() > 8 ? movers.subList(0, 8) : movers;
    }

    private List<Map<String, Object>> enrichMerchantMovers(List<Map<String, Object>> movers, int toYear) {
        for (Map<String, Object> mover : movers) {
            String token = String.valueOf(mover.get("key"));
            String label = String.valueOf(mover.get("label"));
            mover.put("merchantToken", token);
            mover.put("drillDown", drillMerchant(toYear, token, label));
        }
        return movers;
    }

    private List<Map<String, Object>> loadSemanticCategoryRows(int fromYear, int toYear, String userId, LocalDate asOf) {
        List<Map<String, Object>> categoryShifts = new ArrayList<>();
        for (FinanceSemanticMetricsRepository.SemanticTagYearAmount row
                : semanticMetricsRepository.sumExpenseBySemanticTagYears(userId, fromYear, toYear, asOf)) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("year", row.year());
            mapped.put("categoryCode", row.tagId());
            mapped.put("categoryName", FinanceSemanticsCatalog.semanticTagClassification(row.tagId()));
            mapped.put("amount", row.amount());
            categoryShifts.add(mapped);
        }
        return categoryShifts;
    }

    private List<Map<String, Object>> loadCategoryL1Rows(int fromYear, int toYear, String userId, LocalDate asOf) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FinanceSemanticMetricsRepository.CategoryL1YearAmount row
                : semanticMetricsRepository.sumExpenseByCategoryL1Years(userId, fromYear, toYear, asOf)) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("year", row.year());
            mapped.put("categoryCode", row.l1Code());
            mapped.put("categoryName", row.l1Name());
            mapped.put("amount", row.amount());
            rows.add(mapped);
        }
        return rows;
    }

    private enum MatrixDrillMode {
        SEMANTIC_TAG, CATEGORY_L1
    }

    /** Pivot expense rows into years × buckets for multi-year trend tables. */
    private Map<String, Object> buildCategoryYearMatrix(List<Map<String, Object>> categoryRows,
                                                        int fromYear,
                                                        int toYear,
                                                        List<Map<String, Object>> consumptionYearSeries,
                                                        MatrixDrillMode drillMode) {
        List<Integer> years = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            years.add(y);
        }
        Map<Integer, Double> officialTotals = new LinkedHashMap<>();
        Map<Integer, Boolean> partialYears = new LinkedHashMap<>();
        for (Map<String, Object> pt : consumptionYearSeries) {
            int y = ((Number) pt.get("year")).intValue();
            officialTotals.put(y, ((Number) pt.get("amount")).doubleValue());
            partialYears.put(y, Boolean.TRUE.equals(pt.get("partial")));
        }
        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, Map<Integer, Double>> byTag = new LinkedHashMap<>();
        for (Map<String, Object> row : categoryRows) {
            int year = ((Number) row.get("year")).intValue();
            String tagId = String.valueOf(row.get("categoryCode"));
            labels.putIfAbsent(tagId, String.valueOf(row.get("categoryName")));
            double amt = ((Number) row.get("amount")).doubleValue();
            byTag.computeIfAbsent(tagId, k -> new LinkedHashMap<>()).merge(year, amt, Double::sum);
        }
        List<Map<String, Object>> matrixRows = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, Double>> entry : byTag.entrySet()) {
            String tagId = entry.getKey();
            Map<Integer, Double> yearMap = entry.getValue();
            double total = yearMap.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total < 0.01) {
                continue;
            }
            Map<String, Object> amountsByYear = new LinkedHashMap<>();
            for (int y : years) {
                amountsByYear.put(String.valueOf(y), round(yearMap.getOrDefault(y, 0.0)));
            }
            double first = yearMap.getOrDefault(fromYear, 0.0);
            double last = yearMap.getOrDefault(toYear, 0.0);
            int priorYear = toYear - 1;
            double prior = yearMap.getOrDefault(priorYear, 0.0);
            Map<String, Object> shareByYear = new LinkedHashMap<>();
            for (int y : years) {
                double amt = yearMap.getOrDefault(y, 0.0);
                double yearTotal = officialTotals.getOrDefault(y, 0.0);
                shareByYear.put(String.valueOf(y), yearTotal > 0 ? round(amt / yearTotal * 100.0) : 0.0);
            }
            Map<String, Object> matrixRow = new LinkedHashMap<>();
            matrixRow.put("tagId", tagId);
            matrixRow.put("label", labels.getOrDefault(tagId, tagId));
            matrixRow.put("amountsByYear", amountsByYear);
            matrixRow.put("shareByYear", shareByYear);
            matrixRow.put("deltaAmount", round(last - first));
            matrixRow.put("deltaPercent", round(TrendDecomposition.pctChange(first, last)));
            matrixRow.put("yoyPercent", round(TrendDecomposition.pctChange(prior, last)));
            matrixRow.put("drillDown", drillForMatrix(toYear, tagId, labels.getOrDefault(tagId, tagId), drillMode));
            matrixRows.add(matrixRow);
        }
        matrixRows.sort((a, b) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> aYears = (Map<String, Object>) a.get("amountsByYear");
            @SuppressWarnings("unchecked")
            Map<String, Object> bYears = (Map<String, Object>) b.get("amountsByYear");
            double aLast = ((Number) aYears.getOrDefault(String.valueOf(toYear), 0)).doubleValue();
            double bLast = ((Number) bYears.getOrDefault(String.valueOf(toYear), 0)).doubleValue();
            return Double.compare(bLast, aLast);
        });
        Map<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("years", years);
        matrix.put("partialYears", partialYears.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(e -> String.valueOf(e.getKey()))
                .toList());
        matrix.put("rows", matrixRows);
        return matrix;
    }

    private List<Map<String, Object>> buildConsumptionYearSeries(int fromYear, int toYear, LocalDate asOf) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            double amount = consumptionOrIncomeTotal(false, y, toYear, asOf, false);
            boolean partial = AnalyticsDateRange.isPartialConsumptionYear(y, asOf);
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("year", y);
            pt.put("amount", round(amount));
            pt.put("partial", partial);
            if (partial) {
                pt.put("throughDate", asOf.toString());
            }
            series.add(pt);
        }
        return series;
    }

    private double consumptionOrIncomeTotal(boolean income, int year, int toYear, LocalDate asOf, boolean yoyFromYear) {
        AnalyticsDateRange.HalfOpen range = yoyFromYear
                ? AnalyticsDateRange.yoyCompareYearRange(year, toYear, asOf)
                : AnalyticsDateRange.consumptionYearRange(year, asOf);
        LocalDate start = range.startInclusive();
        LocalDate endInc = range.endExclusive().minusDays(1);
        Map<String, BigDecimal> totals = semanticMetricsRepository.aggregateMonth(userKey(), start, endInc);
        return d(income ? totals.get("REAL_INCOME") : totals.get("CONSUMPTION_EXPENSE"));
    }

    private double fixedCostTotal(int year, int toYear, LocalDate asOf, boolean yoyFromYear) {
        AnalyticsDateRange.HalfOpen range = yoyFromYear
                ? AnalyticsDateRange.yoyCompareYearRange(year, toYear, asOf)
                : AnalyticsDateRange.consumptionYearRange(year, asOf);
        LocalDate start = range.startInclusive();
        LocalDate endInc = range.endExclusive().minusDays(1);
        Map<String, BigDecimal> totals = semanticMetricsRepository.aggregateMonth(userKey(), start, endInc);
        return d(totals.get("FIXED_EXPENSE"));
    }

    private Map<String, Double> merchantSpendForYear(int year, String userId, LocalDate asOf, int toYear, boolean yoyFromYear) {
        AnalyticsDateRange.HalfOpen range = yoyFromYear
                ? AnalyticsDateRange.yoyCompareYearRange(year, toYear, asOf)
                : AnalyticsDateRange.consumptionYearRange(year, asOf);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select v.opponent_name, v.transaction_desc, v.amount "
                        + "from v_transaction_finance_semantics v "
                        + "inner join transaction t on t.id = v.id "
                        + "where v.include_in_expense_trend = 1 "
                        + "and v.amount > 0 and v.txn_date >= ? and v.txn_date < ? "
                        + "and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))",
                range.startInclusive(), range.endExclusive(), userId, userId);
        Map<String, Double> totals = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String raw = MerchantNormalizer.rawMerchant(
                    stringVal(row.get("opponent_name")),
                    stringVal(row.get("transaction_desc")));
            String token = MerchantNormalizer.normalizeToken(raw);
            if (token.isEmpty()) {
                continue;
            }
            double amount = ((Number) row.get("amount")).doubleValue();
            totals.merge(token, amount, Double::sum);
        }
        return totals;
    }

    private static double d(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private Map<String, String> merchantLabels(Map<String, Double> from, Map<String, Double> to) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (String token : to.keySet()) {
            labels.put(token, MerchantNormalizer.displayName(token, token));
        }
        for (String token : from.keySet()) {
            labels.putIfAbsent(token, MerchantNormalizer.displayName(token, token));
        }
        return labels;
    }

    private static String buildHeadline(double expenseDelta,
                                        List<Map<String, Object>> categories,
                                        List<Map<String, Object>> merchants) {
        String direction = expenseDelta >= 0 ? "up" : "down";
        String amount = formatMoney(Math.abs(expenseDelta));
        List<String> drivers = new ArrayList<>();
        if (!categories.isEmpty()) {
            drivers.add(String.valueOf(categories.get(0).get("categoryName")));
        }
        if (!merchants.isEmpty()) {
            drivers.add(String.valueOf(merchants.get(0).get("label")));
        }
        if (drivers.isEmpty()) {
            return "Spending is " + direction + " " + amount + " year over year.";
        }
        return "Spending is " + direction + " " + amount + " YoY — mainly "
                + String.join(" and ", drivers) + ".";
    }

    private static Map<String, Object> savingsRateMetric(double from, double to) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", round(from));
        m.put("to", round(to));
        m.put("deltaAmount", round(to - from));
        m.put("deltaPercent", round(to - from));
        return m;
    }

    private static Map<String, String> drillYear(int year, String txnTypes) {
        return Map.of(
                "transactionDateStartStr", "01/01/" + year,
                "transactionDateEndStr", "12/31/" + year,
                "txnTypes", txnTypes);
    }

    private static Map<String, String> drillForMatrix(int year, String code, String label, MatrixDrillMode mode) {
        if (mode == MatrixDrillMode.CATEGORY_L1) {
            return drillCategoryL1(year, code, label);
        }
        return drillSemanticTag(year, code, label);
    }

    private static Map<String, String> drillCategoryL1(int year, String l1Code, String label) {
        Map<String, String> drill = new LinkedHashMap<>(drillYear(year, "expense"));
        drill.put("consumeID", l1Code);
        drill.put("consumeName", label);
        return drill;
    }

    private static Map<String, String> drillSemanticTag(int year, String tagId, String label) {
        Map<String, String> drill = new LinkedHashMap<>(drillYear(year, "expense"));
        drill.put("semanticFilter", tagId);
        return drill;
    }

    private static Map<String, String> drillMerchant(int year, String merchantToken, String merchantLabel) {
        Map<String, String> drill = new LinkedHashMap<>(drillYear(year, "expense"));
        drill.put("merchantToken", merchantToken);
        drill.put("merchantLabel", merchantLabel);
        return drill;
    }

    private static String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String formatMoney(double amount) {
        return "¥" + Math.round(amount);
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
