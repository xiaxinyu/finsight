package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
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

    private DebtTrendAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new DebtTrendAnalysisService(authenticationFacade, semanticMetricsRepository);
        when(authenticationFacade.getUserName()).thenReturn("user1");
    }

    @Test
    void trends_returnsDebtYoYWithMatrices() throws Exception {
        when(semanticMetricsRepository.sumLiabilityFlow(eq("user1"), any(LocalDate.class), any(LocalDate.class), eq("inflow")))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(1);
                    return start.getYear() <= 2024 ? 5000.0 : 3000.0;
                });
        when(semanticMetricsRepository.sumLiabilityFlow(eq("user1"), any(LocalDate.class), any(LocalDate.class), eq("outflow")))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(1);
                    return start.getYear() <= 2024 ? 12000.0 : 18000.0;
                });
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
        Map<String, Object> matrix = (Map<String, Object>) out.get("repaymentTypeMatrix");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) matrix.get("rows");
        assertTrue(rows.size() >= 2);
    }
}
