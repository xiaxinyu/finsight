package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Period-scoped dashboard metrics from {@code v_transaction_finance_semantics}.
 */
@Service
public class PeriodMetricsService {

    private static final DateTimeFormatter US = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final FinanceSemanticMetricsRepository semanticMetricsRepository;
    private final AuthenticationFacade authenticationFacade;

    public PeriodMetricsService(FinanceSemanticMetricsRepository semanticMetricsRepository,
                                AuthenticationFacade authenticationFacade) {
        this.semanticMetricsRepository = semanticMetricsRepository;
        this.authenticationFacade = authenticationFacade;
    }

    public Map<String, Object> periodSummary(String fromStr, String toStr) {
        LocalDate end = parseEnd(toStr);
        LocalDate start = parseStart(fromStr, end);

        Map<String, BigDecimal> totals = semanticMetricsRepository.aggregateMonth(userKey(), start, end);
        double realIncome = d(totals.get("REAL_INCOME"));
        double consumption = d(totals.get("CONSUMPTION_EXPENSE"));
        double net = realIncome - consumption;

        List<Map<String, Object>> months = monthlyTrend(start, end);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("realIncome", realIncome);
        out.put("consumptionExpense", consumption);
        out.put("netCashflow", net);
        out.put("refundInflow", d(totals.get("REFUND_INFLOW")));
        out.put("investmentOutflow", d(totals.get("INVESTMENT_OUTFLOW")));
        out.put("unclassifiedAmount", d(totals.get("UNCLASSIFIED_AMOUNT")));
        out.put("dataQualityScore", d(totals.get("DATA_QUALITY_SCORE")));
        out.put("months", months);
        out.put("metricsSource", "v_transaction_finance_semantics");
        out.put("periodStart", start.toString());
        out.put("periodEnd", end.toString());
        return out;
    }

    private List<Map<String, Object>> monthlyTrend(LocalDate start, LocalDate end) {
        List<Map<String, Object>> months = new ArrayList<>();
        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!cursor.isAfter(last)) {
            LocalDate monthStart = cursor.atDay(1);
            LocalDate monthEnd = cursor.atEndOfMonth();
            LocalDate sliceStart = monthStart.isBefore(start) ? start : monthStart;
            LocalDate sliceEnd = monthEnd.isAfter(end) ? end : monthEnd;
            Map<String, BigDecimal> row = semanticMetricsRepository.aggregateMonth(
                    userKey(), sliceStart, sliceEnd);
            double inc = d(row.get("REAL_INCOME"));
            double exp = d(row.get("CONSUMPTION_EXPENSE"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("yearMonth", cursor.toString());
            m.put("month", cursor.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)));
            m.put("realIncome", inc);
            m.put("consumptionExpense", exp);
            m.put("net", inc - exp);
            months.add(m);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    private static double d(BigDecimal v) {
        return v == null ? 0 : v.doubleValue();
    }

    private static LocalDate parseEnd(String toStr) {
        if (toStr == null || toStr.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(toStr.trim(), US);
    }

    private static LocalDate parseStart(String fromStr, LocalDate end) {
        if (fromStr == null || fromStr.isBlank()) {
            return end.minusMonths(11).withDayOfMonth(1);
        }
        return LocalDate.parse(fromStr.trim(), US);
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
