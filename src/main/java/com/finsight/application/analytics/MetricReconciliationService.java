package com.finsight.application.analytics;

import com.finsight.application.query.TransactionQuery;
import com.finsight.application.query.TransactionQueryAssembler;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.web.api.dto.TransactionParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricReconciliationService {

    private static final double TOLERANCE = 0.001;
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MetricMonthlyRepository metricRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionQuerySupport querySupport;

    public MetricReconciliationService(MetricMonthlyRepository metricRepository,
                                       TransactionRepository transactionRepository,
                                       TransactionQuerySupport querySupport) {
        this.metricRepository = metricRepository;
        this.transactionRepository = transactionRepository;
        this.querySupport = querySupport;
    }

    public Map<String, Object> reconcile(String userId, String yearMonth) throws Exception {
        YearMonth ym = YearMonth.parse(yearMonth, YM);
        TransactionParam param = new TransactionParam();
        param.setTransactionDateStartStr(String.format("%02d/01/%04d", ym.getMonthValue(), ym.getYear()));
        param.setTransactionDateEndStr(String.format("%02d/%02d/%04d",
                ym.getMonthValue(), ym.lengthOfMonth(), ym.getYear()));
        TransactionQuery q = TransactionQueryAssembler.from(param);
        querySupport.enrich(q);

        int idx = ym.getMonthValue() - 1;
        double reportIncome = parse(monthValue(transactionRepository.monthIncomeReport(q), idx));
        double reportExpense = parse(monthValue(transactionRepository.monthExpenseReport(q), idx));

        BigDecimal metricIncome = metricRepository.find(userId, yearMonth, MetricCode.INCOME_TOTAL.name());
        BigDecimal metricExpense = metricRepository.find(userId, yearMonth, MetricCode.EXPENSE_TOTAL.name());

        List<String> mismatches = new ArrayList<>();
        if (!withinTolerance(reportIncome, metricIncome)) {
            mismatches.add("INCOME_TOTAL report=" + reportIncome + " metric=" + metricIncome);
        }
        if (!withinTolerance(reportExpense, metricExpense)) {
            mismatches.add("EXPENSE_TOTAL report=" + reportExpense + " metric=" + metricExpense);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("yearMonth", yearMonth);
        out.put("reportIncome", reportIncome);
        out.put("reportExpense", reportExpense);
        out.put("metricIncome", metricIncome);
        out.put("metricExpense", metricExpense);
        out.put("mismatches", mismatches);
        out.put("ok", mismatches.isEmpty());
        return out;
    }

    private static String monthValue(List<KeyValue> rows, int idx) {
        if (rows == null || idx < 0 || idx >= rows.size()) {
            return "0";
        }
        return rows.get(idx).getValue();
    }

    private static double parse(String v) {
        try {
            return v == null ? 0 : Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static boolean withinTolerance(double report, BigDecimal metric) {
        if (metric == null) {
            return report == 0;
        }
        double m = metric.doubleValue();
        if (report == 0 && m == 0) {
            return true;
        }
        double base = Math.max(Math.abs(report), 1);
        return Math.abs(report - m) / base <= TOLERANCE;
    }
}
