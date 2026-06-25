package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.common.exception.AppServiceException;
import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.infrastructure.mapper.FinancialMapper;
import com.finsight.web.api.dto.TransactionParam;
import org.springframework.scheduling.annotation.Async;
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

@Service
public class MetricMonthlyService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MetricMonthlyRepository metricRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionQuerySupport querySupport;
    private final FinancialMapper financialMapper;
    private final AuthenticationFacade authenticationFacade;
    private final FinanceSemanticMetricsRepository semanticMetricsRepository;

    public MetricMonthlyService(MetricMonthlyRepository metricRepository,
                                TransactionRepository transactionRepository,
                                TransactionQuerySupport querySupport,
                                FinancialMapper financialMapper,
                                AuthenticationFacade authenticationFacade,
                                FinanceSemanticMetricsRepository semanticMetricsRepository) {
        this.metricRepository = metricRepository;
        this.transactionRepository = transactionRepository;
        this.querySupport = querySupport;
        this.financialMapper = financialMapper;
        this.authenticationFacade = authenticationFacade;
        this.semanticMetricsRepository = semanticMetricsRepository;
    }

    @Async
    public void refreshAsync(String yearMonth, String userId) {
        String userKey = normalizeUserKey(userId);
        try {
            refreshForUser(yearMonth, userKey);
        } catch (AppServiceException ex) {
            throw new IllegalStateException("Metric refresh failed for " + yearMonth, ex);
        }
    }

    /** Schedules refresh using the current security principal (call from request thread). */
    public void refreshAsync(String yearMonth) {
        refreshAsync(yearMonth, resolveUserKeyOrAnonymous());
    }

    public Map<String, BigDecimal> refresh(String yearMonth) throws AppServiceException {
        return refreshForUser(yearMonth, resolveUserKeyOrAnonymous());
    }

    private Map<String, BigDecimal> refreshForUser(String yearMonth, String userId) throws AppServiceException {
        Map<String, BigDecimal> computed = computeMonth(yearMonth, userId);
        Map<String, BigDecimal> written = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : computed.entrySet()) {
            metricRepository.upsert(userId, yearMonth, e.getKey(), e.getValue());
            written.put(e.getKey(), e.getValue());
        }
        return written;
    }

    public List<Map<String, Object>> history(String fromYm, String toYm) {
        return metricRepository.listForUser(resolveUserKeyOrAnonymous(), fromYm, toYm);
    }

    /** @deprecated Diagnostic-only — do not use on user read paths; prefer {@link #history}. */
    @Deprecated
    public List<Map<String, Object>> historyFromReports(String fromYm, String toYm) throws AppServiceException {
        YearMonth start = YearMonth.parse(fromYm, YM);
        YearMonth end = YearMonth.parse(toYm, YM);
        List<Map<String, Object>> out = new ArrayList<>();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String label = ym.format(YM);
            for (Map.Entry<String, BigDecimal> e : computeMonth(label, resolveUserKeyOrAnonymous()).entrySet()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("yearMonth", label);
                row.put("metricCode", e.getKey());
                row.put("metricValue", e.getValue());
                out.add(row);
            }
        }
        return out;
    }

    private Map<String, BigDecimal> computeMonth(String yearMonth, String userId) throws AppServiceException {
        YearMonth ym = YearMonth.parse(yearMonth, YM);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        TransactionParam param = new TransactionParam();
        param.setTransactionDateStartStr(String.format("%02d/%02d/%04d", start.getMonthValue(), start.getDayOfMonth(), start.getYear()));
        param.setTransactionDateEndStr(String.format("%02d/%02d/%04d", end.getMonthValue(), end.getDayOfMonth(), end.getYear()));
        TransactionQuery q = TransactionQueryAssembler.from(param);
        querySupport.enrich(q);
        double income = sumReport(transactionRepository.monthIncomeReport(q), ym.getMonthValue() - 1);
        double expense = sumReport(transactionRepository.monthExpenseReport(q), ym.getMonthValue() - 1);
        double net = income - expense;
        double savingsRate = income > 0 ? net / income : 0;
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        out.put(MetricCode.INCOME_TOTAL.name(), bd(income));
        out.put(MetricCode.EXPENSE_TOTAL.name(), bd(expense));
        out.put(MetricCode.NET_CASHFLOW.name(), bd(net));
        out.put(MetricCode.SAVINGS_RATE.name(), bd(savingsRate));

        Map<String, BigDecimal> semantic = semanticMetricsRepository.aggregateMonth(
                userId, start, end);
        for (Map.Entry<String, BigDecimal> e : semantic.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        if (!semantic.containsKey(MetricCode.UNCLASSIFIED_COUNT.name())) {
            out.put(MetricCode.UNCLASSIFIED_COUNT.name(), bd(financialMapper.countUnclassified()));
        }
        return out;
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static double sumReport(List<KeyValue> rows, int monthIndex) {
        if (rows == null || rows.isEmpty() || monthIndex < 0 || monthIndex >= rows.size()) {
            return 0;
        }
        String v = rows.get(monthIndex).getValue();
        try {
            return v == null ? 0 : Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String normalizeUserKey(String userId) {
        return userId == null || userId.isBlank() ? "_anonymous" : userId;
    }

    private String resolveUserKeyOrAnonymous() {
        try {
            return normalizeUserKey(authenticationFacade.getUserName());
        } catch (RuntimeException ex) {
            return "_anonymous";
        }
    }
}
