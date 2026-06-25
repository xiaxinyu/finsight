package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.port.MetricMonthlyRepository;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceSemanticMetricsTest {

    @Mock
    private MetricMonthlyRepository metricRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionQuerySupport querySupport;
    @Mock
    private FinancialMapper financialMapper;
    @Mock
    private AuthenticationFacade authenticationFacade;
    @Mock
    private FinanceSemanticMetricsRepository semanticMetricsRepository;

    @InjectMocks
    private MetricMonthlyService metricMonthlyService;

    @Test
    void refresh_persistsSemanticMetricsAlongsideLegacyTotals() throws Exception {
        when(authenticationFacade.getUserName()).thenReturn("alice");
        doAnswer(inv -> null).when(querySupport).enrich(any());
        when(transactionRepository.monthIncomeReport(any())).thenReturn(monthSeries("12000"));
        when(transactionRepository.monthExpenseReport(any())).thenReturn(monthSeries("8000"));

        Map<String, BigDecimal> semantic = new LinkedHashMap<>();
        semantic.put("REAL_INCOME", new BigDecimal("10000.0000"));
        semantic.put("CONSUMPTION_EXPENSE", new BigDecimal("7500.0000"));
        semantic.put("REFUND_INFLOW", new BigDecimal("500.0000"));
        semantic.put("UNCLASSIFIED_COUNT", new BigDecimal("3.0000"));
        when(semanticMetricsRepository.aggregateMonth(eq("alice"), any(), any())).thenReturn(semantic);

        Map<String, BigDecimal> written = metricMonthlyService.refresh("2026-06");

        assertEquals(new BigDecimal("12000.0000"), written.get("INCOME_TOTAL"));
        assertEquals(new BigDecimal("10000.0000"), written.get("REAL_INCOME"));
        assertEquals(new BigDecimal("7500.0000"), written.get("CONSUMPTION_EXPENSE"));
        assertEquals(new BigDecimal("500.0000"), written.get("REFUND_INFLOW"));
        verify(metricRepository).upsert(eq("alice"), eq("2026-06"), eq("REAL_INCOME"), eq(new BigDecimal("10000.0000")));
        verify(metricRepository).upsert(eq("alice"), eq("2026-06"), eq("CONSUMPTION_EXPENSE"),
                eq(new BigDecimal("7500.0000")));
    }

    @Test
    void metricValuesPreferring_usesRealIncomeWhenPresent() {
        List<Map<String, Object>> metrics = List.of(
                metricRow("2026-01", "REAL_INCOME", 9000),
                metricRow("2026-01", "INCOME_TOTAL", 12000),
                metricRow("2026-01", "CONSUMPTION_EXPENSE", 7000),
                metricRow("2026-01", "EXPENSE_TOTAL", 8000));

        List<Double> incomes = FinancialProfileService.metricValuesPreferring(metrics, "REAL_INCOME", "INCOME_TOTAL");
        List<Double> expenses = FinancialProfileService.metricValuesPreferring(
                metrics, "CONSUMPTION_EXPENSE", "EXPENSE_TOTAL");

        assertEquals(9000.0, incomes.get(0));
        assertEquals(7000.0, expenses.get(0));
    }

    @Test
    void metricValuesPreferring_fallsBackWhenSemanticMissing() {
        List<Map<String, Object>> metrics = List.of(
                metricRow("2026-01", "INCOME_TOTAL", 12000),
                metricRow("2026-01", "EXPENSE_TOTAL", 8000));

        List<Double> incomes = FinancialProfileService.metricValuesPreferring(metrics, "REAL_INCOME", "INCOME_TOTAL");
        assertEquals(12000.0, incomes.get(0));
        assertTrue(incomes.size() == 1);
    }

    private static Map<String, Object> metricRow(String month, String code, double value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("yearMonth", month);
        row.put("metricCode", code);
        row.put("metricValue", value);
        return row;
    }

    private static List<KeyValue> monthSeries(String juneValue) {
        KeyValue[] months = new KeyValue[12];
        for (int i = 0; i < 12; i++) {
            KeyValue kv = new KeyValue();
            kv.setKey("M" + i);
            kv.setValue(i == 5 ? juneValue : "0");
            months[i] = kv;
        }
        return List.of(months);
    }
}
