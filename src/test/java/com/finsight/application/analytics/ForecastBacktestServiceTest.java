package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.domain.model.MetricCode;
import com.finsight.domain.port.MetricMonthlyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastBacktestServiceTest {

    @Mock
    private MetricMonthlyRepository metricRepository;
    @Mock
    private AuthenticationFacade authenticationFacade;

    private ForecastBacktestService service;

    @BeforeEach
    void setUp() {
        service = new ForecastBacktestService(metricRepository, authenticationFacade);
        when(authenticationFacade.getUserName()).thenReturn("tester");
    }

    @Test
    void backtest_computesMapeFromCutoffProjection() {
        YearMonth end = YearMonth.now().minusMonths(1);
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String month = end.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            rows.add(row(month, MetricCode.INCOME_TOTAL.name(), 8000 + i * 100));
            rows.add(row(month, MetricCode.EXPENSE_TOTAL.name(), 5000 + i * 50));
        }
        when(metricRepository.listForUser(eq("tester"), anyString(), anyString())).thenReturn(rows);

        Map<String, Object> out = service.backtest(3);

        assertNotNull(out.get("incomeMape"));
        assertEquals("cutoff_hybrid_projection", out.get("method"));
        assertNotNull(out.get("expenseMape"));
        assertNotNull(out.get("incomeMae"));
        assertNotNull(out.get("coverage"));
        assertTrue(((Number) out.get("incomeMape")).doubleValue() >= 0);
    }

    @Test
    void backtest_marksInsufficientSampleWhenEmpty() {
        when(metricRepository.listForUser(eq("tester"), anyString(), anyString())).thenReturn(List.of());
        Map<String, Object> out = service.backtest(3);
        assertEquals(true, out.get("insufficientSample"));
        assertEquals("low", out.get("confidenceLevel"));
    }

    private static Map<String, Object> row(String ym, String code, double value) {
        return Map.of("yearMonth", ym, "metricCode", code, "metricValue", value);
    }
}
