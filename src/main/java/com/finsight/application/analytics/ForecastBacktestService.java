package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ForecastBacktestService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final double COVERAGE_TOLERANCE = 0.25;

    private final MetricMonthlyRepository metricRepository;
    private final AuthenticationFacade authenticationFacade;

    public ForecastBacktestService(MetricMonthlyRepository metricRepository,
                                   AuthenticationFacade authenticationFacade) {
        this.metricRepository = metricRepository;
        this.authenticationFacade = authenticationFacade;
    }

    public Map<String, Object> backtest(int months) {
        int cap = months <= 0 ? 6 : Math.min(months, 24);
        String userId = userKey();
        YearMonth end = YearMonth.now().minusMonths(1);
        YearMonth start = end.minusMonths(cap - 1L);
        List<Map<String, Object>> history = metricRepository.listForUser(
                userId, start.minusMonths(24).format(YM), end.format(YM));

        Map<String, Double> actualIncome = metricMap(history, MetricCode.INCOME_TOTAL.name());
        Map<String, Double> actualExpense = metricMap(history, MetricCode.EXPENSE_TOTAL.name());
        List<ForecastProjection.YearMonthValue> incomeSeries = ForecastProjection.toSeries(actualIncome);
        List<ForecastProjection.YearMonthValue> expenseSeries = ForecastProjection.toSeries(actualExpense);

        List<Map<String, Object>> rows = new ArrayList<>();
        double incomeMapeSum = 0;
        double expenseMapeSum = 0;
        double incomeMaeSum = 0;
        double expenseMaeSum = 0;
        double incomeBiasSum = 0;
        double expenseBiasSum = 0;
        int incomeCount = 0;
        int expenseCount = 0;
        int coverageHits = 0;
        int coverageTotal = 0;

        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            String month = cursor.format(YM);
            double ai = actualIncome.getOrDefault(month, 0.0);
            double ae = actualExpense.getOrDefault(month, 0.0);
            double fi = ForecastProjection.projectMonth(
                    ForecastProjection.priorValues(incomeSeries, cursor),
                    cursor.getMonthValue(),
                    incomeSeries,
                    1.0);
            double fe = ForecastProjection.projectMonth(
                    ForecastProjection.priorValues(expenseSeries, cursor),
                    cursor.getMonthValue(),
                    expenseSeries,
                    1.0);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("monthKey", month);
            item.put("actualIncome", ai);
            item.put("forecastIncome", round(fi));
            item.put("incomeErrorPct", pctError(ai, fi));
            item.put("actualExpense", ae);
            item.put("forecastExpense", round(fe));
            item.put("expenseErrorPct", pctError(ae, fe));
            rows.add(item);

            if (ai > 0 && fi > 0) {
                incomeMapeSum += Math.abs(ai - fi) / ai;
                incomeMaeSum += Math.abs(ai - fi);
                incomeBiasSum += (fi - ai) / ai;
                incomeCount++;
                coverageTotal++;
                if (Math.abs(ai - fi) / ai <= COVERAGE_TOLERANCE) {
                    coverageHits++;
                }
            }
            if (ae > 0 && fe > 0) {
                expenseMapeSum += Math.abs(ae - fe) / ae;
                expenseMaeSum += Math.abs(ae - fe);
                expenseBiasSum += (fe - ae) / ae;
                expenseCount++;
                coverageTotal++;
                if (Math.abs(ae - fe) / ae <= COVERAGE_TOLERANCE) {
                    coverageHits++;
                }
            }
            cursor = cursor.plusMonths(1);
        }

        int evalPoints = incomeCount + expenseCount;
        boolean insufficientSample = evalPoints < 3;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("months", rows);
        out.put("method", "cutoff_hybrid_projection");
        out.put("incomeMape", incomeCount == 0 ? null : round(incomeMapeSum / incomeCount));
        out.put("expenseMape", expenseCount == 0 ? null : round(expenseMapeSum / expenseCount));
        out.put("incomeMae", incomeCount == 0 ? null : round(incomeMaeSum / incomeCount));
        out.put("expenseMae", expenseCount == 0 ? null : round(expenseMaeSum / expenseCount));
        out.put("incomeBias", incomeCount == 0 ? null : round(incomeBiasSum / incomeCount));
        out.put("expenseBias", expenseCount == 0 ? null : round(expenseBiasSum / expenseCount));
        out.put("coverage", coverageTotal == 0 ? null : round((double) coverageHits / coverageTotal));
        out.put("sampleMonths", rows.size());
        out.put("evalPoints", evalPoints);
        out.put("insufficientSample", insufficientSample);
        out.put("confidenceLevel", insufficientSample ? "low" : (evalPoints >= 6 ? "medium" : "low"));
        if (insufficientSample) {
            out.put("message", "Not enough forecast vs actual pairs to evaluate accuracy.");
        }
        return out;
    }

    private static Map<String, Double> metricMap(List<Map<String, Object>> history, String code) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map<String, Object> row : history) {
            if (!code.equals(String.valueOf(row.get("metricCode")))) {
                continue;
            }
            String month = String.valueOf(row.get("yearMonth"));
            Object val = row.get("metricValue");
            if (val instanceof Number n) {
                out.put(month, n.doubleValue());
            }
        }
        return out;
    }

    private static Double pctError(double actual, double forecast) {
        if (actual <= 0 || forecast <= 0) {
            return null;
        }
        return round(Math.abs(actual - forecast) / actual * 100);
    }

    private static double round(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private String userKey() {
        String user = authenticationFacade.getUserName();
        return user == null || user.isBlank() ? "_anonymous" : user;
    }
}
