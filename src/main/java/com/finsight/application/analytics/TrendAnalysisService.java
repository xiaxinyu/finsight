package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.infrastructure.mapper.FinancialMapper;
import com.finsight.web.api.dto.TransactionParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrendAnalysisService {

    private final TransactionRepository transactionRepository;
    private final TransactionQuerySupport querySupport;
    private final AuthenticationFacade authenticationFacade;
    private final FinancialMapper financialMapper;
    private final JdbcTemplate jdbcTemplate;

    public TrendAnalysisService(TransactionRepository transactionRepository,
                                TransactionQuerySupport querySupport,
                                AuthenticationFacade authenticationFacade,
                                FinancialMapper financialMapper,
                                JdbcTemplate jdbcTemplate) {
        this.transactionRepository = transactionRepository;
        this.querySupport = querySupport;
        this.authenticationFacade = authenticationFacade;
        this.financialMapper = financialMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> trends(int fromYear, int toYear) throws Exception {
        String userId = userKey();
        double incomeFrom = yearTotal("income", fromYear);
        double incomeTo = yearTotal("income", toYear);
        double expenseFrom = yearTotal("expense", fromYear);
        double expenseTo = yearTotal("expense", toYear);
        double fixedFrom = safeFixedYear(fromYear);
        double fixedTo = safeFixedYear(toYear);

        double incomeDelta = incomeTo - incomeFrom;
        double expenseDelta = expenseTo - expenseFrom;
        double savingsFrom = incomeFrom > 0 ? (incomeFrom - expenseFrom) / incomeFrom * 100 : 0;
        double savingsTo = incomeTo > 0 ? (incomeTo - expenseTo) / incomeTo * 100 : 0;

        List<Map<String, Object>> categoryRows = loadCategoryRows(fromYear, toYear);
        List<Map<String, Object>> topCategoryGrowth = enrichCategoryMovers(categoryRows, fromYear, toYear, expenseDelta);

        Map<String, Double> merchantFrom = merchantSpendByYear(fromYear, userId);
        Map<String, Double> merchantTo = merchantSpendByYear(toYear, userId);
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
        out.put("savingsInflection", Map.of(
                "fromYear", fromYear,
                "toYear", toYear,
                "fromRate", round(savingsFrom),
                "toRate", round(savingsTo),
                "deltaPercent", round(savingsTo - savingsFrom)));
        out.put("lifestyleInflation", lifestyleInflation);
        out.put("trends", trendItems);
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
                    drillCategory(toYear, String.valueOf(cat.get("categoryCode")), String.valueOf(cat.get("categoryName")))));
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
            String code = String.valueOf(row.get("categoryCode"));
            names.putIfAbsent(code, String.valueOf(row.get("categoryName")));
            double amt = ((Number) row.get("amount")).doubleValue();
            if (year == fromYear) {
                from.merge(code, amt, Double::sum);
            }
            if (year == toYear) {
                to.merge(code, amt, Double::sum);
            }
        }
        List<Map<String, Object>> movers = new ArrayList<>();
        for (String code : to.keySet()) {
            double start = from.getOrDefault(code, 0.0);
            double end = to.getOrDefault(code, 0.0);
            double delta = end - start;
            double pct = TrendDecomposition.pctChange(start, end);
            if (Math.abs(pct) < 10 && Math.abs(delta) < 100) {
                continue;
            }
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("categoryCode", code);
            g.put("categoryName", names.getOrDefault(code, code));
            g.put("fromAmount", round(start));
            g.put("toAmount", round(end));
            g.put("pctChange", Math.round(pct));
            g.put("deltaAmount", round(delta));
            g.put("deltaPercent", round(pct));
            g.put("contributionPct", round(TrendDecomposition.contributionPct(delta, expenseDelta)));
            g.put("drillDown", drillCategory(toYear, code, names.getOrDefault(code, code)));
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

    private List<Map<String, Object>> loadCategoryRows(int fromYear, int toYear) throws Exception {
        List<Map<String, Object>> categoryShifts = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            final int year = y;
            TransactionParam param = new TransactionParam();
            param.setTransactionDateStartStr("01/01/" + year);
            param.setTransactionDateEndStr("12/31/" + year);
            param.setTxnTypes("expense");
            TransactionQuery q = TransactionQueryAssembler.from(param);
            querySupport.enrich(q);
            transactionRepository.consumeReport(q).forEach(cat -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("year", year);
                row.put("categoryCode", cat.getCode());
                row.put("categoryName", cat.getName());
                row.put("amount", cat.getValue());
                categoryShifts.add(row);
            });
        }
        return categoryShifts;
    }

    private double yearTotal(String txnTypes, int year) throws Exception {
        TransactionParam param = new TransactionParam();
        param.setTransactionDateStartStr("01/01/" + year);
        param.setTransactionDateEndStr("12/31/" + year);
        param.setTxnTypes(txnTypes);
        TransactionQuery q = TransactionQueryAssembler.from(param);
        querySupport.enrich(q);
        List<KeyValue> rows = "income".equals(txnTypes)
                ? transactionRepository.monthIncomeReport(q)
                : transactionRepository.monthExpenseReport(q);
        return rows.stream().mapToDouble(r -> Double.parseDouble(String.valueOf(r.getValue()))).sum();
    }

    private double safeFixedYear(int year) {
        Double value = financialMapper.sumFixedBucketYear(year);
        return value == null ? 0 : value;
    }

    private Map<String, Double> merchantSpendByYear(int year, String userId) {
        AnalyticsDateRange.HalfOpen range = AnalyticsDateRange.calendarYear(year);
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

    private static Map<String, String> drillCategory(int year, String categoryCode, String categoryName) {
        Map<String, String> drill = new LinkedHashMap<>(drillYear(year, "expense"));
        drill.put("consumeID", categoryCode);
        drill.put("consumeName", categoryName);
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
