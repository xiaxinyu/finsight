package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodMetricsServiceTest {

    @Mock
    private FinanceSemanticMetricsRepository semanticMetricsRepository;
    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private PeriodMetricsService service;

    @Test
    void periodSummary_aggregatesSemanticTotalsAndNet() {
        when(authenticationFacade.getUserName()).thenReturn("alice");
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("REAL_INCOME", new BigDecimal("10000"));
        totals.put("CONSUMPTION_EXPENSE", new BigDecimal("7000"));
        totals.put("REFUND_INFLOW", new BigDecimal("200"));
        totals.put("DATA_QUALITY_SCORE", new BigDecimal("95"));
        when(semanticMetricsRepository.aggregateMonth(eq("alice"), any(), any())).thenReturn(totals);

        Map<String, Object> out = service.periodSummary("01/01/2026", "06/30/2026");

        assertEquals(10000.0, out.get("realIncome"));
        assertEquals(7000.0, out.get("consumptionExpense"));
        assertEquals(3000.0, out.get("netCashflow"));
        assertEquals("v_transaction_finance_semantics", out.get("metricsSource"));
    }
}
