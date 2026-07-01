package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.classification.FinanceSemanticsCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DebtTrendAnalysisService {

    private final AuthenticationFacade authenticationFacade;
    private final FinanceSemanticMetricsRepository semanticMetricsRepository;

    public DebtTrendAnalysisService(AuthenticationFacade authenticationFacade,
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

        double borrowingFrom = liabilityFlowTotal(fromYear, toYear, asOf, true, true);
        double borrowingTo = liabilityFlowTotal(toYear, toYear, asOf, true, false);
        double repaymentFrom = liabilityFlowTotal(fromYear, toYear, asOf, false, true);
        double repaymentTo = liabilityFlowTotal(toYear, toYear, asOf, false, false);
        double netFrom = borrowingFrom - repaymentFrom;
        double netTo = borrowingTo - repaymentTo;

        double borrowingPct = TrendDecomposition.pctChange(borrowingFrom, borrowingTo);
        double repaymentPct = TrendDecomposition.pctChange(repaymentFrom, repaymentTo);
        double netDelta = netTo - netFrom;
        boolean debtPressure = debtPressureDetected(borrowingPct, repaymentPct, repaymentTo - repaymentFrom, netDelta);

        List<Map<String, Object>> debtYearSeries = buildDebtYearSeries(matrixFrom, toYear, asOf);
        List<Map<String, Object>> repaymentRows = loadTagRows(matrixFrom, toYear, userId, asOf, "outflow");
        List<Map<String, Object>> borrowingRows = loadTagRows(matrixFrom, toYear, userId, asOf, "inflow");
        List<Map<String, Object>> topRepaymentGrowth = enrichTypeMovers(
                repaymentRows, fromYear, toYear, repaymentTo - repaymentFrom);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("borrowing", TrendDecomposition.deltaMetric(borrowingFrom, borrowingTo));
        summary.put("repayment", TrendDecomposition.deltaMetric(repaymentFrom, repaymentTo));
        summary.put("netFlow", TrendDecomposition.deltaMetric(netFrom, netTo));
        summary.put("headline", buildHeadline(repaymentTo - repaymentFrom, netDelta, topRepaymentGrowth));

        Map<String, Object> debtPressureBlock = new LinkedHashMap<>();
        debtPressureBlock.put("detected", debtPressure);
        debtPressureBlock.put("borrowingPctChange", round(borrowingPct));
        debtPressureBlock.put("repaymentPctChange", round(repaymentPct));
        debtPressureBlock.put("gapPct", round(repaymentPct - borrowingPct));
        debtPressureBlock.put("note", debtPressure
                ? "Repayments grew faster than new borrowing — review loan balances and payment plans."
                : "Debt cash flows look stable or improving versus last year.");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fromYear", fromYear);
        out.put("toYear", toYear);
        out.put("historyFromYear", matrixFrom);
        out.put("compareMode", ytdCompare ? "ytd_aligned" : "full_year");
        out.put("summary", summary);
        out.put("debtYearSeries", debtYearSeries);
        out.put("repaymentTypeMatrix", buildTypeYearMatrix(
                repaymentRows, matrixFrom, toYear, debtYearSeries, "repayment"));
        out.put("borrowingTypeMatrix", buildTypeYearMatrix(
                borrowingRows, matrixFrom, toYear, debtYearSeries, "borrowing"));
        out.put("topRepaymentGrowth", topRepaymentGrowth);
        out.put("debtPressure", debtPressureBlock);
        out.put("metricsSource", "v_transaction_finance_semantics.economic_nature=liability");
        out.put("user", userId);
        return out;
    }

    private List<Map<String, Object>> loadTagRows(int fromYear, int toYear, String userId,
                                                   LocalDate asOf, String direction) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FinanceSemanticMetricsRepository.LiabilityTagYearAmount row
                : semanticMetricsRepository.sumLiabilityBySemanticTagYears(
                        userId, fromYear, toYear, asOf, direction)) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("year", row.year());
            mapped.put("categoryCode", row.tagId());
            mapped.put("categoryName", FinanceSemanticsCatalog.semanticTagClassification(row.tagId()));
            mapped.put("amount", row.amount());
            rows.add(mapped);
        }
        return rows;
    }

    private List<Map<String, Object>> buildDebtYearSeries(int fromYear, int toYear, LocalDate asOf) {
        Map<Integer, FinanceSemanticMetricsRepository.LiabilityYearFlow> byYear = new LinkedHashMap<>();
        for (FinanceSemanticMetricsRepository.LiabilityYearFlow flow
                : semanticMetricsRepository.sumLiabilityFlowByYear(userKey(), fromYear, toYear, asOf)) {
            byYear.put(flow.year(), flow);
        }
        List<Map<String, Object>> series = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            FinanceSemanticMetricsRepository.LiabilityYearFlow flow = byYear.get(y);
            double borrowing = flow == null ? 0.0 : flow.borrowing();
            double repayment = flow == null ? 0.0 : flow.repayment();
            boolean partial = AnalyticsDateRange.isPartialConsumptionYear(y, asOf);
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("year", y);
            pt.put("borrowing", round(borrowing));
            pt.put("repayment", round(repayment));
            pt.put("net", round(borrowing - repayment));
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
                                                    List<Map<String, Object>> debtYearSeries,
                                                    String flowKind) {
        List<Integer> years = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            years.add(y);
        }
        Map<Integer, Double> officialTotals = new LinkedHashMap<>();
        Map<Integer, Boolean> partialYears = new LinkedHashMap<>();
        for (Map<String, Object> pt : debtYearSeries) {
            int y = ((Number) pt.get("year")).intValue();
            double total = "borrowing".equals(flowKind)
                    ? ((Number) pt.get("borrowing")).doubleValue()
                    : ((Number) pt.get("repayment")).doubleValue();
            officialTotals.put(y, total);
            partialYears.put(y, Boolean.TRUE.equals(pt.get("partial")));
        }
        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, Map<Integer, Double>> byTag = new LinkedHashMap<>();
        for (Map<String, Object> row : tagRows) {
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
            matrixRow.put("drillDown", drillLiabilityTag(toYear, tagId, labels.getOrDefault(tagId, tagId)));
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
                                                       double repaymentDelta) {
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
            g.put("contributionPct", round(TrendDecomposition.contributionPct(delta, repaymentDelta)));
            g.put("drillDown", drillLiabilityTag(toYear, tagId, label));
            movers.add(g);
        }
        movers.sort((a, b) -> Double.compare(
                Math.abs(((Number) b.get("deltaAmount")).doubleValue()),
                Math.abs(((Number) a.get("deltaAmount")).doubleValue())));
        return movers.size() > 8 ? movers.subList(0, 8) : movers;
    }

    private double liabilityFlowTotal(int year, int toYear, LocalDate asOf, boolean borrowing, boolean yoyFromYear) {
        AnalyticsDateRange.HalfOpen range = yoyFromYear
                ? AnalyticsDateRange.yoyCompareYearRange(year, toYear, asOf)
                : AnalyticsDateRange.consumptionYearRange(year, asOf);
        LocalDate endInc = range.endExclusive().minusDays(1);
        String direction = borrowing ? "inflow" : "outflow";
        return semanticMetricsRepository.sumLiabilityFlow(userKey(), range.startInclusive(), endInc, direction);
    }

    private static boolean debtPressureDetected(double borrowingPct,
                                                double repaymentPct,
                                                double repaymentDelta,
                                                double netDelta) {
        if (repaymentDelta < 500.0 || repaymentDelta <= 0) {
            return netDelta < -1000.0;
        }
        return repaymentPct - borrowingPct >= 10.0 || netDelta < -1000.0;
    }

    private static String buildHeadline(double repaymentDelta,
                                        double netDelta,
                                        List<Map<String, Object>> topTypes) {
        if (Math.abs(repaymentDelta) < 1 && Math.abs(netDelta) < 1) {
            return "No significant debt-related cash flows in the comparison period.";
        }
        String repayDir = repaymentDelta >= 0 ? "up" : "down";
        String repayAmt = formatMoney(Math.abs(repaymentDelta));
        StringBuilder sb = new StringBuilder("Repayments are ")
                .append(repayDir).append(' ').append(repayAmt).append(" year over year");
        if (!topTypes.isEmpty()) {
            sb.append(" — mainly ").append(topTypes.get(0).get("categoryName"));
        }
        sb.append('.');
        if (netDelta < -500) {
            sb.append(" Net borrowing exceeded repayments (debt load increased).");
        } else if (netDelta > 500) {
            sb.append(" Net repayments exceeded new borrowing.");
        }
        return sb.toString();
    }

    private static Map<String, String> drillLiabilityTag(int year, String tagId, String label) {
        Map<String, String> drill = new LinkedHashMap<>();
        drill.put("transactionDateStartStr", "01/01/" + year);
        drill.put("transactionDateEndStr", "12/31/" + year);
        drill.put("txnTypes", "finance");
        drill.put("semanticFilter", tagId);
        drill.put("consumeName", label);
        return drill;
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
