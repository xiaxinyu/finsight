package com.finsight.application.analytics;

import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import com.finsight.domain.port.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricReconciliationServiceTest {

    @Mock
    private MetricMonthlyRepository metricRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionQuerySupport querySupport;

    @InjectMocks
    private MetricReconciliationService service;

    @Test
    void reconcile_okWhenReportMatchesMetric() throws Exception {
        doAnswer(inv -> null).when(querySupport).enrich(any());
        when(transactionRepository.monthIncomeReport(any())).thenReturn(monthSeries("1000"));
        when(transactionRepository.monthExpenseReport(any())).thenReturn(monthSeries("800"));
        when(metricRepository.find("u1", "2025-06", MetricCode.INCOME_TOTAL.name())).thenReturn(new BigDecimal("1000"));
        when(metricRepository.find("u1", "2025-06", MetricCode.EXPENSE_TOTAL.name())).thenReturn(new BigDecimal("800"));

        Map<String, Object> result = service.reconcile("u1", "2025-06");
        assertTrue((Boolean) result.get("ok"));
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
