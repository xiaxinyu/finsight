package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.classification.FinanceSemanticsCatalog;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncomeTrendAnalysisService {

    private final AuthenticationFacade authenticationFacade;
    private final FinanceSemanticMetricsRepository semanticMetricsRepository;

    public IncomeTrendAnalysisService(AuthenticationFacade authenticationFacade,
                                      FinanceSemanticMetricsRepository semanticMetricsRepository) {
        this.authenticationFacade = authenticationFacade;
        this.semanticMetricsRepository = semanticMetricsRepository;
    }

    public Map<String, Object> trends(int fromYear, int toYear) throws Exception {
        return trends(fromYear, toYear, fromYear);
    }

    public Map<String, Object> trends(int fromYear, int toYear, int historyFromYear) throws Exception {
        String userId = userKey();
        LocalDate asOf = LocalDate.now();
        int matrixFrom = Math.min(historyFromYear, fromYear);
        boolean ytdCompare = toYear == asOf.getYear();

        double totalFrom = incomeTotal(fromYear, toYear, asOf, true);
        double totalTo = incomeTotal(toYear, toYear, asOf, false);
        double totalDelta = totalTo - totalFrom;
        double totalPct = TrendDecomposition.pctChange(totalFrom, totalTo);

        double realFrom = tagTotal(fromYear, toYear, asOf, "real_income", true);
        double realTo = tagTotal(toYear, toYear, asOf, "real_income", false);
        double investFrom = tagTotal(fromYear, toYear, asOf, "investment_income", true);
        double investTo = tagTotal(toYear, toYear, asOf, "investment_income", false);
        double otherFrom = tagTotal(fromYear, toYear, asOf, "other_income", true);
        double otherTo = tagTotal(toYear, toYear, asOf, "other_income", false);

        List<Map<String, Object>> matrixRows = loadSemanticRows(matrixFrom, toYear, userId, asOf);
        List<Map<String, Object>> l1Rows = loadL1Rows(matrixFrom, toYear, userId, asOf);
        List<Map<String, Object>> incomeYearSeries = buildIncomeYearSeries(matrixFrom, toYear, asOf);
        List<Map<String, Object>> topIncomeGrowth = enrichTypeMovers(matrixRows, fromYear, toYear, totalDelta);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalIncome", TrendDecomposition.deltaMetric(totalFrom, totalTo));
        summary.put("realIncome", TrendDecomposition.deltaMetric(realFrom, realTo));
        summary.put("investmentIncome", TrendDecomposition.deltaMetric(investFrom, investTo));
        summary.put("otherIncome", TrendDecomposition.deltaMetric(otherFrom, otherTo));
        summary.put("headline", buildHeadline(totalDelta, totalPct, topIncomeGrowth));

        Map<String, Object> momentum = new LinkedHashMap<>();
        momentum.put("detected", totalDelta > 500 && totalPct >= 5.0);
        momentum.put("totalPctChange", round(totalPct));
        momentum.put("realPctChange", round(TrendDecomposition.pctChange(realFrom, realTo)));
        momentum.put("note", totalDelta >= 0
                ? "Income is up year over year — check which sources drove the change."
                : "Income declined versus last year — review salary, bonuses, and side income.");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fromYear", fromYear);
        out.put("toYear", toYear);
        out.put("historyFromYear", matrixFrom);
        out.put("compareMode", ytdCompare ? "ytd_aligned" : "full_year");
        out.put("summary", summary);
        out.put("incomeYearSeries", incomeYearSeries);
        out.put("incomeTypeMatrix", buildTypeYearMatrix(matrixRows, matrixFrom, toYear, incomeYearSeries));
        out.put("categoryL1YearMatrix", buildL1YearMatrix(l1Rows, matrixFrom, toYear, incomeYearSeries));
        out.put("topIncomeGrowth", topIncomeGrowth);
        out.put("incomeMomentum", momentum);
        out.put("metricsSource", "v_transaction_finance_semantics.include_in_income_trend");
        out.put("user", userId);
        return out;
    }

    private double incomeTotal(int year, int toYear, LocalDate asOf, boolean yoyFromYear) {
        AnalyticsDateRange.HalfOpen range = yoyFromYear
                ? AnalyticsDateRange.yoyCompareYearRange(year, toYear, asOf)
                : AnalyticsDateRange.consumptionYearRange(year, asOf);
        LocalDate endInc = range.endExclusive().minusDays(1);
        Map<String, BigDecimal> totals = semanticMetricsRepository.aggregateMonth(userKey(), range.startInclusive(), endInc);
        return d(totals.get("REAL_INCOME"));
    }

    private double tagTotal(int year, int toYear, LocalDate asOf, String tagId, boolean yoyFromYear) {
        AnalyticsDateRange.HalfOpen range = yoyFromYear
                ? AnalyticsDateRange.yoyCompareYearRange(year, toYear, asOf)
                : AnalyticsDateRange.consumptionYearRange(year, asOf);
        LocalDate start = range.startInclusive();
        LocalDate endInc = range.endExclusive().minusDays(1);
        double total = 0;
        for (FinanceSemanticMetricsRepository.SemanticTagYearAmount row
                : semanticMetricsRepository.sumIncomeBySemanticTagYears(
                        userKey(), start.getYear(), endInc.getYear(), asOf)) {
            if (row.year() == year && tagId.equals(row.tagId())) {
                total += row.amount();
            }
        }
        return total;
    }

    private List<Map<String, Object>> loadSemanticRows(int fromYear, int toYear, String userId, LocalDate asOf) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FinanceSemanticMetricsRepository.SemanticTagYearAmount row
                : semanticMetricsRepository.sumIncomeBySemanticTagYears(userId, fromYear, toYear, asOf)) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("year", row.year());
            mapped.put("categoryCode", row.tagId());
            mapped.put("categoryName", FinanceSemanticsCatalog.semanticTagClassification(row.tagId()));
            mapped.put("amount", row.amount());
            rows.add(mapped);
        }
        return rows;
    }

    private List<Map<String, Object>> loadL1Rows(int fromYear, int toYear, String userId, LocalDate asOf) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FinanceSemanticMetricsRepository.CategoryL1YearAmount row
                : semanticMetricsRepository.sumIncomeByCategoryL1Years(userId, fromYear, toYear, asOf)) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("year", row.year());
            mapped.put("categoryCode", row.l1Code());
            mapped.put("categoryName", row.l1Name());
            mapped.put("amount", row.amount());
            rows.add(mapped);
        }
        return rows;
    }

    private List<Map<String, Object>> buildIncomeYearSeries(int fromYear, int toYear, LocalDate asOf) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            double amount = incomeTotal(y, toYear, asOf, false);
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

    private Map<String, Object> buildTypeYearMatrix(List<Map<String, Object>> tagRows,
                                                    int fromYear,
                                                    int toYear,
                                                    List<Map<String, Object>> incomeYearSeries) {
        return buildYearMatrix(tagRows, fromYear, toYear, incomeYearSeries, MatrixDrillMode.SEMANTIC_TAG);
    }

    private Map<String, Object> buildL1YearMatrix(List<Map<String, Object>> l1Rows,
                                                  int fromYear,
                                                  int toYear,
                                                  List<Map<String, Object>> incomeYearSeries) {
        return buildYearMatrix(l1Rows, fromYear, toYear, incomeYearSeries, MatrixDrillMode.CATEGORY_L1);
    }

    private enum MatrixDrillMode {
        SEMANTIC_TAG, CATEGORY_L1
    }

    private Map<String, Object> buildYearMatrix(List<Map<String, Object>> rows,
                                                int fromYear,
                                                int toYear,
                                                List<Map<String, Object>> yearSeries,
                                                MatrixDrillMode drillMode) {
        List<Integer> years = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            years.add(y);
        }
        Map<Integer, Double> officialTotals = new LinkedHashMap<>();
        Map<Integer, Boolean> partialYears = new LinkedHashMap<>();
        for (Map<String, Object> pt : yearSeries) {
            int y = ((Number) pt.get("year")).intValue();
            officialTotals.put(y, ((Number) pt.get("amount")).doubleValue());
            partialYears.put(y, Boolean.TRUE.equals(pt.get("partial")));
        }
        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, Map<Integer, Double>> byTag = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
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

    private List<Map<String, Object>> enrichTypeMovers(List<Map<String, Object>> rows,
                                                       int fromYear,
                                                       int toYear,
                                                       double totalDelta) {
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
            g.put("label", label);
            g.put("fromAmount", round(start));
            g.put("toAmount", round(end));
            g.put("pctChange", Math.round(pct));
            g.put("deltaAmount", round(delta));
            g.put("deltaPercent", round(pct));
            g.put("contributionPct", round(TrendDecomposition.contributionPct(delta, totalDelta)));
            g.put("drillDown", drillSemanticTag(toYear, tagId));
            movers.add(g);
        }
        movers.sort((a, b) -> Double.compare(
                Math.abs(((Number) b.get("deltaAmount")).doubleValue()),
                Math.abs(((Number) a.get("deltaAmount")).doubleValue())));
        return movers.size() > 8 ? movers.subList(0, 8) : movers;
    }

    private static String buildHeadline(double totalDelta, double totalPct, List<Map<String, Object>> topTypes) {
        if (Math.abs(totalDelta) < 1) {
            return "No significant income change in the comparison period.";
        }
        String dir = totalDelta >= 0 ? "up" : "down";
        String amt = formatMoney(Math.abs(totalDelta));
        StringBuilder sb = new StringBuilder("Income is ")
                .append(dir).append(' ').append(amt)
                .append(" (").append(totalPct >= 0 ? "+" : "").append(String.format("%.1f", totalPct)).append("%) year over year");
        if (!topTypes.isEmpty()) {
            sb.append(" — mainly ").append(topTypes.get(0).get("categoryName"));
        }
        return sb.append('.').toString();
    }

    private static Map<String, String> drillForMatrix(int year, String code, String label, MatrixDrillMode mode) {
        if (mode == MatrixDrillMode.CATEGORY_L1) {
            Map<String, String> drill = new LinkedHashMap<>(drillYear(year));
            drill.put("consumeID", code);
            return drill;
        }
        return drillSemanticTag(year, code);
    }

    private static Map<String, String> drillYear(int year) {
        return Map.of(
                "transactionDateStartStr", "01/01/" + year,
                "transactionDateEndStr", "12/31/" + year,
                "txnTypes", "income");
    }

    private static Map<String, String> drillSemanticTag(int year, String tagId) {
        Map<String, String> drill = new LinkedHashMap<>(drillYear(year));
        drill.put("semanticFilter", tagId);
        return drill;
    }

    private static double d(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
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
