package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.classification.FinanceSemanticsCatalog;
import com.finsight.application.finance.UserScopedFinancialQueries;
import com.finsight.domain.model.Loan;
import com.finsight.domain.model.LoanLenderYearFlow;
import com.finsight.domain.port.LoanRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DebtTrendAnalysisService {

    private static final double FLAT_THRESHOLD = 500.0;

    private final AuthenticationFacade authenticationFacade;
    private final FinanceSemanticMetricsRepository semanticMetricsRepository;
    private final UserScopedFinancialQueries scopedFinancialQueries;
    private final LoanRepository loanRepository;

    public DebtTrendAnalysisService(AuthenticationFacade authenticationFacade,
                                    FinanceSemanticMetricsRepository semanticMetricsRepository,
                                    UserScopedFinancialQueries scopedFinancialQueries,
                                    LoanRepository loanRepository) {
        this.authenticationFacade = authenticationFacade;
        this.semanticMetricsRepository = semanticMetricsRepository;
        this.scopedFinancialQueries = scopedFinancialQueries;
        this.loanRepository = loanRepository;
    }

    public Map<String, Object> trends(int fromYear, int toYear) throws Exception {
        return trends(fromYear, toYear, fromYear);
    }

    public Map<String, Object> trends(int fromYear, int toYear, int historyFromYear) throws Exception {
        String userId = userKey();
        LocalDate asOf = LocalDate.now();
        int matrixFrom = Math.min(historyFromYear, fromYear);
        boolean ytdCompare = toYear == asOf.getYear();

        List<Map<String, Object>> debtYearSeries = buildDebtYearSeries(matrixFrom, toYear, asOf);
        mergeLoanLinkFlows(debtYearSeries, userId, matrixFrom, toYear, asOf);
        double loanOutstanding = loanRepository.sumActiveOutstanding(userId).doubleValue();
        int activeLoanCount = countActiveLoans(userId);
        boolean loanLedgerPrimary = loanOutstanding > 0 || activeLoanCount > 0;
        if (loanLedgerPrimary) {
            mergeLoanRepaymentEstimates(debtYearSeries, userId, matrixFrom, toYear, asOf);
        }
        double borrowingTo = yearFlow(debtYearSeries, toYear, "borrowing");
        double repaymentTo = yearFlow(debtYearSeries, toYear, "repayment");
        double borrowingFrom;
        double repaymentFrom;
        if (ytdCompare) {
            borrowingFrom = liabilityFlowTotal(fromYear, toYear, asOf, true, true);
            repaymentFrom = liabilityFlowTotal(fromYear, toYear, asOf, false, true);
            if (loanLedgerPrimary) {
                repaymentFrom = Math.max(repaymentFrom, loanRepaymentEstimate(userId, fromYear, toYear, asOf));
                repaymentTo = Math.max(repaymentTo, loanRepaymentEstimate(userId, toYear, toYear, asOf));
            }
        } else {
            borrowingFrom = yearFlow(debtYearSeries, fromYear, "borrowing");
            repaymentFrom = yearFlow(debtYearSeries, fromYear, "repayment");
        }
        double netFrom = borrowingFrom - repaymentFrom;
        double netTo = borrowingTo - repaymentTo;

        double borrowingPct = TrendDecomposition.pctChange(borrowingFrom, borrowingTo);
        double repaymentPct = TrendDecomposition.pctChange(repaymentFrom, repaymentTo);
        double netDelta = netTo - netFrom;
        boolean debtPressure = debtPressureDetected(borrowingPct, repaymentPct, repaymentTo - repaymentFrom, netDelta);

        double cardLiabilities = scopedFinancialQueries.sumCurrentLiabilities();
        double anchorBalance = loanLedgerPrimary
                ? loanOutstanding + cardLiabilities
                : cardLiabilities;
        attachEstimatedBalances(debtYearSeries, loanLedgerPrimary ? loanOutstanding : anchorBalance);
        List<Map<String, Object>> repaymentRows = loadTagRows(matrixFrom, toYear, userId, asOf, "outflow");
        List<Map<String, Object>> borrowingRows = loadTagRows(matrixFrom, toYear, userId, asOf, "inflow");
        List<Map<String, Object>> topRepaymentGrowth = enrichTypeMovers(
                repaymentRows, fromYear, toYear, repaymentTo - repaymentFrom);

        Map<String, Object> loanLedger = buildLoanLedgerBlock(userId, matrixFrom, toYear, asOf);
        Map<String, Object> loanRepaymentMatrix = buildLoanLenderMatrix(
                userId, matrixFrom, toYear, asOf, debtYearSeries, "repayment");
        Map<String, Object> loanBorrowingMatrix = buildLoanLenderMatrix(
                userId, matrixFrom, toYear, asOf, debtYearSeries, "borrowing");

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
        out.put("debtBalance", buildDebtBalanceBlock(
                debtYearSeries, anchorBalance, loanOutstanding, cardLiabilities,
                loanLedgerPrimary, asOf, matrixFrom));
        out.put("repaymentTypeMatrix", buildTypeYearMatrix(
                repaymentRows, matrixFrom, toYear, debtYearSeries, "repayment"));
        out.put("borrowingTypeMatrix", buildTypeYearMatrix(
                borrowingRows, matrixFrom, toYear, debtYearSeries, "borrowing"));
        out.put("loanLedger", loanLedger);
        out.put("loanRepaymentMatrix", loanRepaymentMatrix);
        out.put("loanBorrowingMatrix", loanBorrowingMatrix);
        out.put("topRepaymentGrowth", topRepaymentGrowth);
        out.put("debtPressure", debtPressureBlock);
        out.put("metricsSource", loanLedgerPrimary
                ? "fin_loan ledger + v_transaction_finance_semantics (credit cards)"
                : "v_transaction_finance_semantics.economic_nature=liability");
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
        double cumulative = 0;
        Double priorNet = null;
        for (int y = fromYear; y <= toYear; y++) {
            FinanceSemanticMetricsRepository.LiabilityYearFlow flow = byYear.get(y);
            double borrowing = flow == null ? 0.0 : flow.borrowing();
            double repayment = flow == null ? 0.0 : flow.repayment();
            double net = borrowing - repayment;
            cumulative += net;
            boolean partial = AnalyticsDateRange.isPartialConsumptionYear(y, asOf);
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("year", y);
            pt.put("borrowing", round(borrowing));
            pt.put("repayment", round(repayment));
            pt.put("net", round(net));
            pt.put("cumulativeNet", round(cumulative));
            pt.put("debtDirection", debtDirection(net));
            if (priorNet != null) {
                pt.put("yoyNetDelta", round(net - priorNet));
            }
            pt.put("partial", partial);
            if (partial) {
                pt.put("throughDate", asOf.toString());
            }
            series.add(pt);
            priorNet = net;
        }
        return series;
    }

    private static double yearFlow(List<Map<String, Object>> series, int year, String field) {
        for (Map<String, Object> pt : series) {
            if (((Number) pt.get("year")).intValue() == year) {
                return ((Number) pt.get(field)).doubleValue();
            }
        }
        return 0.0;
    }

    private static void attachEstimatedBalances(List<Map<String, Object>> series, double anchorBalance) {
        if (series.isEmpty()) {
            return;
        }
        double endBalance = Math.max(0, anchorBalance);
        for (int i = series.size() - 1; i >= 0; i--) {
            Map<String, Object> pt = series.get(i);
            double net = ((Number) pt.get("net")).doubleValue();
            pt.put("estimatedBalance", round(endBalance));
            endBalance = Math.max(0, endBalance - net);
        }
    }

    private Map<String, Object> buildDebtBalanceBlock(List<Map<String, Object>> series,
                                                      double totalLiabilities,
                                                      double loanOutstanding,
                                                      double cardLiabilities,
                                                      boolean loanLedgerPrimary,
                                                      LocalDate asOf,
                                                      int historyFromYear) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("currentLiabilities", round(totalLiabilities));
        block.put("loanOutstanding", round(loanOutstanding));
        block.put("creditCardLiabilities", round(cardLiabilities));
        block.put("asOfDate", asOf.toString());
        block.put("historyFromYear", historyFromYear);
        block.put("source", loanLedgerPrimary ? "loan_ledger" : "bank_card_balances");
        block.put("note", loanLedgerPrimary
                ? "Outstanding loans from Ledgers → Loans (fin_loan). "
                + (cardLiabilities > 0 ? "Credit card balances are included in the total. " : "")
                + "Historical chart points are estimated from classified cash flows."
                : "Year-end balances are estimated by working back from current credit/loan card balances "
                + "using classified borrowing and repayment flows.");
        if (!series.isEmpty()) {
            double balanceAnchor = loanLedgerPrimary ? loanOutstanding : totalLiabilities;
            double balanceBeforeFirst = balanceAnchor;
            for (int i = series.size() - 1; i >= 0; i--) {
                double net = ((Number) series.get(i).get("net")).doubleValue();
                balanceBeforeFirst -= net;
            }
            double periodStart = Math.max(0, balanceBeforeFirst);
            double periodChange = balanceAnchor - periodStart;
            block.put("periodStartBalance", round(periodStart));
            block.put("periodBalanceChange", round(periodChange));
            block.put("periodTrend", periodChange > FLAT_THRESHOLD ? "increase"
                    : periodChange < -FLAT_THRESHOLD ? "decrease" : "flat");
        }
        return block;
    }

    private int countActiveLoans(String userId) {
        int count = 0;
        for (Loan loan : loanRepository.listActive(userId)) {
            if (!"CLOSED".equalsIgnoreCase(loan.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private Map<String, Object> buildLoanLedgerBlock(String userId, int fromYear, int toYear, LocalDate asOf) {
        List<Loan> loans = loanRepository.listActive(userId);
        List<Map<String, Object>> lenders = new ArrayList<>();
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalMonthly = BigDecimal.ZERO;
        double weightedRate = 0;
        double weightSum = 0;
        int activeCount = 0;
        for (Loan loan : loans) {
            if ("CLOSED".equalsIgnoreCase(loan.getStatus())) {
                continue;
            }
            activeCount++;
            BigDecimal outstanding = loan.getOutstandingBalance() != null
                    ? loan.getOutstandingBalance()
                    : loan.getPrincipalAmount();
            if (outstanding != null) {
                totalOutstanding = totalOutstanding.add(outstanding);
            }
            if (loan.getMonthlyPayment() != null) {
                totalMonthly = totalMonthly.add(loan.getMonthlyPayment());
            }
            if (loan.getInterestRatePct() != null && outstanding != null) {
                double bal = outstanding.doubleValue();
                weightedRate += loan.getInterestRatePct().doubleValue() * bal;
                weightSum += bal;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("loanId", loan.getId());
            row.put("lenderName", loan.getLenderName());
            row.put("outstandingBalance", outstanding == null ? 0 : round(outstanding.doubleValue()));
            row.put("monthlyPayment", loan.getMonthlyPayment() == null ? 0 : round(loan.getMonthlyPayment().doubleValue()));
            row.put("interestRatePct", loan.getInterestRatePct() == null ? null : round(loan.getInterestRatePct().doubleValue()));
            row.put("linkCount", loan.getLinkCount() == null ? 0 : loan.getLinkCount());
            row.put("maturityDate", loan.getMaturityDate());
            lenders.add(row);
        }
        lenders.sort((a, b) -> Double.compare(
                ((Number) b.get("outstandingBalance")).doubleValue(),
                ((Number) a.get("outstandingBalance")).doubleValue()));

        Map<String, Object> block = new LinkedHashMap<>();
        block.put("activeLoanCount", activeCount);
        block.put("totalOutstanding", round(totalOutstanding.doubleValue()));
        block.put("totalMonthlyPayment", round(totalMonthly.doubleValue()));
        block.put("weightedAvgRatePct", weightSum > 0 ? round(weightedRate / weightSum) : 0);
        block.put("lenders", lenders);
        block.put("annualizedRepaymentEstimate", round(estimatedAnnualRepayment(userId, toYear, asOf)));
        return block;
    }

    private double estimatedAnnualRepayment(String userId, int year, LocalDate asOf) {
        double months = AnalyticsDateRange.isPartialConsumptionYear(year, asOf)
                ? asOf.getMonthValue()
                : 12.0;
        return loanRepository.sumActiveMonthlyPayment(userId).doubleValue() * months;
    }

    private void mergeLoanLinkFlows(List<Map<String, Object>> series,
                                    String userId,
                                    int fromYear,
                                    int toYear,
                                    LocalDate asOf) {
        LocalDate rangeEnd = AnalyticsDateRange.isPartialConsumptionYear(toYear, asOf)
                ? asOf.plusDays(1)
                : LocalDate.of(toYear + 1, 1, 1);
        List<LoanLenderYearFlow> flows = loanRepository.sumLinkFlowByLenderYear(
                userId, Date.valueOf(LocalDate.of(fromYear, 1, 1)), Date.valueOf(rangeEnd));
        if (flows.isEmpty()) {
            return;
        }
        Map<Integer, double[]> byYear = new LinkedHashMap<>();
        for (LoanLenderYearFlow flow : flows) {
            double amt = flow.amount() == null ? 0 : flow.amount().doubleValue();
            double[] acc = byYear.computeIfAbsent(flow.year(), y -> new double[2]);
            if ("DISBURSEMENT".equalsIgnoreCase(flow.linkType())) {
                acc[0] += amt;
            } else if ("REPAYMENT".equalsIgnoreCase(flow.linkType()) || "INTEREST".equalsIgnoreCase(flow.linkType())) {
                acc[1] += amt;
            }
        }
        for (Map<String, Object> pt : series) {
            int y = ((Number) pt.get("year")).intValue();
            double[] linked = byYear.get(y);
            if (linked == null) {
                continue;
            }
            double borrowing = ((Number) pt.get("borrowing")).doubleValue() + linked[0];
            double repayment = ((Number) pt.get("repayment")).doubleValue() + linked[1];
            double net = borrowing - repayment;
            pt.put("borrowing", round(borrowing));
            pt.put("repayment", round(repayment));
            pt.put("net", round(net));
            pt.put("debtDirection", debtDirection(net));
            pt.put("hasLoanLinks", true);
        }
        recalculateSeriesDerivatives(series);
    }

    private void mergeLoanRepaymentEstimates(List<Map<String, Object>> series,
                                             String userId,
                                             int fromYear,
                                             int toYear,
                                             LocalDate asOf) {
        LocalDate rangeEnd = AnalyticsDateRange.isPartialConsumptionYear(toYear, asOf)
                ? asOf.plusDays(1)
                : LocalDate.of(toYear + 1, 1, 1);
        List<LoanLenderYearFlow> linkFlows = loanRepository.sumLinkFlowByLenderYear(
                userId, Date.valueOf(LocalDate.of(fromYear, 1, 1)), Date.valueOf(rangeEnd));
        Map<String, Map<Integer, Double>> linkedByLoanYear = new LinkedHashMap<>();
        for (LoanLenderYearFlow flow : linkFlows) {
            if (!"REPAYMENT".equalsIgnoreCase(flow.linkType())
                    && !"INTEREST".equalsIgnoreCase(flow.linkType())) {
                continue;
            }
            String key = flow.loanId() != null ? flow.loanId() : flow.lenderName();
            double amt = flow.amount() == null ? 0 : flow.amount().doubleValue();
            linkedByLoanYear.computeIfAbsent(key, k -> new LinkedHashMap<>()).merge(flow.year(), amt, Double::sum);
        }

        Map<Integer, Double> estimateByYear = new LinkedHashMap<>();
        for (Loan loan : loanRepository.listActive(userId)) {
            if ("CLOSED".equalsIgnoreCase(loan.getStatus()) || loan.getMonthlyPayment() == null) {
                continue;
            }
            String key = loan.getId();
            Map<Integer, Double> linkedYears = linkedByLoanYear.getOrDefault(key, Map.of());
            for (Map<String, Object> pt : series) {
                int y = ((Number) pt.get("year")).intValue();
                double linked = linkedYears.getOrDefault(y, 0.0);
                double estimate = linked > 0
                        ? linked
                        : loan.getMonthlyPayment().doubleValue() * loanRepaymentMonths(y, toYear, asOf);
                estimateByYear.merge(y, estimate, Double::sum);
            }
        }

        for (Map<String, Object> pt : series) {
            int y = ((Number) pt.get("year")).intValue();
            double estimate = estimateByYear.getOrDefault(y, 0.0);
            if (estimate < 0.01) {
                continue;
            }
            double current = ((Number) pt.get("repayment")).doubleValue();
            if (estimate <= current + 0.01) {
                continue;
            }
            double borrowing = ((Number) pt.get("borrowing")).doubleValue();
            double net = borrowing - estimate;
            pt.put("repayment", round(estimate));
            pt.put("net", round(net));
            pt.put("debtDirection", debtDirection(net));
            pt.put("hasLoanEstimate", true);
        }
        recalculateSeriesDerivatives(series);
    }

    private double loanRepaymentEstimate(String userId, int year, int toYear, LocalDate asOf) {
        AnalyticsDateRange.HalfOpen range = ytdCompareFromYear(year, toYear, asOf)
                ? AnalyticsDateRange.yoyCompareYearRange(year, toYear, asOf)
                : AnalyticsDateRange.consumptionYearRange(year, asOf);
        List<LoanLenderYearFlow> linkFlows = loanRepository.sumLinkFlowByLenderYear(
                userId, Date.valueOf(range.startInclusive()), Date.valueOf(range.endExclusive()));
        Map<String, Double> linkedByLoan = new LinkedHashMap<>();
        for (LoanLenderYearFlow flow : linkFlows) {
            if (flow.year() != year) {
                continue;
            }
            if (!"REPAYMENT".equalsIgnoreCase(flow.linkType())
                    && !"INTEREST".equalsIgnoreCase(flow.linkType())) {
                continue;
            }
            String key = flow.loanId() != null ? flow.loanId() : flow.lenderName();
            double amt = flow.amount() == null ? 0 : flow.amount().doubleValue();
            linkedByLoan.merge(key, amt, Double::sum);
        }

        double total = 0;
        double months = loanRepaymentMonths(year, toYear, asOf);
        for (Loan loan : loanRepository.listActive(userId)) {
            if ("CLOSED".equalsIgnoreCase(loan.getStatus()) || loan.getMonthlyPayment() == null) {
                continue;
            }
            double linked = linkedByLoan.getOrDefault(loan.getId(), 0.0);
            total += linked > 0 ? linked : loan.getMonthlyPayment().doubleValue() * months;
        }
        return total;
    }

    private static boolean ytdCompareFromYear(int year, int toYear, LocalDate asOf) {
        return year == toYear - 1 && toYear == asOf.getYear();
    }

    private static double loanRepaymentMonths(int year, int toYear, LocalDate asOf) {
        if (AnalyticsDateRange.isPartialConsumptionYear(year, asOf)) {
            return asOf.getMonthValue();
        }
        if (ytdCompareFromYear(year, toYear, asOf)) {
            return AnalyticsDateRange.alignedPriorYearDay(toYear, asOf).getMonthValue();
        }
        return 12.0;
    }

    private static void recalculateSeriesDerivatives(List<Map<String, Object>> series) {
        double cumulative = 0;
        Double priorNet = null;
        for (Map<String, Object> pt : series) {
            double net = ((Number) pt.get("net")).doubleValue();
            cumulative += net;
            pt.put("cumulativeNet", round(cumulative));
            if (priorNet != null) {
                pt.put("yoyNetDelta", round(net - priorNet));
            }
            priorNet = net;
        }
    }

    private Map<String, Object> buildLoanLenderMatrix(String userId,
                                                        int fromYear,
                                                        int toYear,
                                                        LocalDate asOf,
                                                        List<Map<String, Object>> debtYearSeries,
                                                        String flowKind) {
        List<Integer> years = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            years.add(y);
        }
        LocalDate rangeEnd = AnalyticsDateRange.isPartialConsumptionYear(toYear, asOf)
                ? asOf.plusDays(1)
                : LocalDate.of(toYear + 1, 1, 1);
        List<LoanLenderYearFlow> linkFlows = loanRepository.sumLinkFlowByLenderYear(
                userId, Date.valueOf(LocalDate.of(fromYear, 1, 1)), Date.valueOf(rangeEnd));

        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, Map<Integer, Double>> byLender = new LinkedHashMap<>();

        for (LoanLenderYearFlow flow : linkFlows) {
            boolean disbursement = "DISBURSEMENT".equalsIgnoreCase(flow.linkType());
            boolean repayment = "REPAYMENT".equalsIgnoreCase(flow.linkType())
                    || "INTEREST".equalsIgnoreCase(flow.linkType());
            if ("borrowing".equals(flowKind) && !disbursement) {
                continue;
            }
            if ("repayment".equals(flowKind) && !repayment) {
                continue;
            }
            String key = flow.loanId() != null ? flow.loanId() : flow.lenderName();
            labels.putIfAbsent(key, flow.lenderName());
            double amt = flow.amount() == null ? 0 : flow.amount().doubleValue();
            byLender.computeIfAbsent(key, k -> new LinkedHashMap<>()).merge(flow.year(), amt, Double::sum);
        }

        for (Loan loan : loanRepository.listActive(userId)) {
            if ("CLOSED".equalsIgnoreCase(loan.getStatus())) {
                continue;
            }
            String key = loan.getId();
            labels.putIfAbsent(key, loan.getLenderName());
            if (!"repayment".equals(flowKind) || loan.getMonthlyPayment() == null) {
                continue;
            }
            Map<Integer, Double> yearMap = byLender.computeIfAbsent(key, k -> new LinkedHashMap<>());
            for (int y : years) {
                if (yearMap.getOrDefault(y, 0.0) > 0) {
                    continue;
                }
                yearMap.put(y, loan.getMonthlyPayment().doubleValue() * loanRepaymentMonths(y, toYear, asOf));
            }
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

        List<Map<String, Object>> matrixRows = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, Double>> entry : byLender.entrySet()) {
            String key = entry.getKey();
            Map<Integer, Double> yearMap = entry.getValue();
            double total = yearMap.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total < 0.01) {
                continue;
            }
            Map<String, Object> amountsByYear = new LinkedHashMap<>();
            Map<String, Object> shareByYear = new LinkedHashMap<>();
            for (int y : years) {
                double amt = yearMap.getOrDefault(y, 0.0);
                amountsByYear.put(String.valueOf(y), round(amt));
                double yearTotal = officialTotals.getOrDefault(y, 0.0);
                shareByYear.put(String.valueOf(y), yearTotal > 0 ? round(amt / yearTotal * 100.0) : 0.0);
            }
            double first = yearMap.getOrDefault(fromYear, 0.0);
            double last = yearMap.getOrDefault(toYear, 0.0);
            Map<String, Object> matrixRow = new LinkedHashMap<>();
            matrixRow.put("tagId", key);
            matrixRow.put("label", labels.getOrDefault(key, key));
            matrixRow.put("amountsByYear", amountsByYear);
            matrixRow.put("shareByYear", shareByYear);
            matrixRow.put("deltaAmount", round(last - first));
            matrixRow.put("deltaPercent", round(TrendDecomposition.pctChange(first, last)));
            matrixRow.put("yoyPercent", round(TrendDecomposition.pctChange(
                    yearMap.getOrDefault(toYear - 1, 0.0), last)));
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
        matrix.put("source", "fin_loan");
        return matrix;
    }

    private static String debtDirection(double net) {
        if (net > FLAT_THRESHOLD) {
            return "increase";
        }
        if (net < -FLAT_THRESHOLD) {
            return "decrease";
        }
        return "flat";
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
