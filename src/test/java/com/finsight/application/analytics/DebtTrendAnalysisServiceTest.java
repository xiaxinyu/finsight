package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.finance.UserScopedFinancialQueries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtTrendAnalysisServiceTest {

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private FinanceSemanticMetricsRepository semanticMetricsRepository;

    @Mock
    private UserScopedFinancialQueries scopedFinancialQueries;

    private DebtTrendAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new DebtTrendAnalysisService(authenticationFacade, semanticMetricsRepository, scopedFinancialQueries);
        when(authenticationFacade.getUserName()).thenReturn("user1");
        when(scopedFinancialQueries.sumCurrentLiabilities()).thenReturn(50000.0);
    }

    @Test
    void trends_returnsDebtYoYWithMatrices() throws Exception {
        when(semanticMetricsRepository.sumLiabilityFlowByYear(eq("user1"), eq(2024), eq(2025), any()))
                .thenReturn(List.of(
                        new FinanceSemanticMetricsRepository.LiabilityYearFlow(2024, 5000, 12000),
                        new FinanceSemanticMetricsRepository.LiabilityYearFlow(2025, 3000, 18000)));
        when(semanticMetricsRepository.sumLiabilityBySemanticTagYears(
                eq("user1"), eq(2024), eq(2025), any(), eq("outflow")))
                .thenReturn(List.of(
                        new FinanceSemanticMetricsRepository.LiabilityTagYearAmount("finance_loan", 2024, 8000),
                        new FinanceSemanticMetricsRepository.LiabilityTagYearAmount("finance_credit_loan", 2024, 4000),
                        new FinanceSemanticMetricsRepository.LiabilityTagYearAmount("finance_loan", 2025, 12000),
                        new FinanceSemanticMetricsRepository.LiabilityTagYearAmount("finance_credit_loan", 2025, 6000)));
        when(semanticMetricsRepository.sumLiabilityBySemanticTagYears(
                eq("user1"), eq(2024), eq(2025), any(), eq("inflow")))
                .thenReturn(List.of(
                        new FinanceSemanticMetricsRepository.LiabilityTagYearAmount("finance_loan", 2024, 5000),
                        new FinanceSemanticMetricsRepository.LiabilityTagYearAmount("finance_loan", 2025, 3000)));

        Map<String, Object> out = service.trends(2024, 2025);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) out.get("summary");
        @SuppressWarnings("unchecked")
        Map<String, Object> repayment = (Map<String, Object>) summary.get("repayment");
        assertEquals(6000.0, ((Number) repayment.get("deltaAmount")).doubleValue());
        assertNotNull(out.get("debtYearSeries"));
        @SuppressWarnings("unchecked")
        Map<String, Object> balance = (Map<String, Object>) out.get("debtBalance");
        assertEquals(50000.0, ((Number) balance.get("currentLiabilities")).doubleValue());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) out.get("debtYearSeries");
        assertEquals("decrease", series.get(1).get("debtDirection"));
        assertEquals(50000.0, ((Number) series.get(1).get("estimatedBalance")).doubleValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> matrix = (Map<String, Object>) out.get("repaymentTypeMatrix");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) matrix.get("rows");
        assertTrue(rows.size() >= 2);
    }
}
