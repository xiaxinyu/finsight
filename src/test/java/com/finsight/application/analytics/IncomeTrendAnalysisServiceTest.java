package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeTrendAnalysisServiceTest {

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private FinanceSemanticMetricsRepository semanticMetricsRepository;

    private IncomeTrendAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new IncomeTrendAnalysisService(authenticationFacade, semanticMetricsRepository);
        when(authenticationFacade.getUserName()).thenReturn("user1");
    }

    @Test
    void trends_returnsIncomeYoYWithMatrices() throws Exception {
        when(semanticMetricsRepository.aggregateMonth(eq("user1"), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(1);
                    return aggregate(start.getYear() <= 2024 ? 100000 : 120000);
                });
        when(semanticMetricsRepository.sumIncomeBySemanticTagYears(eq("user1"), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(List.of(
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("real_income", 2024, 90000),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("investment_income", 2024, 10000),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("real_income", 2025, 105000),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("investment_income", 2025, 15000)));
        when(semanticMetricsRepository.sumIncomeByCategoryL1Years(eq("user1"), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(List.of(
                        new FinanceSemanticMetricsRepository.CategoryL1YearAmount("INC", "Income", 2024, 100000),
                        new FinanceSemanticMetricsRepository.CategoryL1YearAmount("INC", "Income", 2025, 120000)));

        Map<String, Object> out = service.trends(2024, 2025);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) out.get("summary");
        @SuppressWarnings("unchecked")
        Map<String, Object> total = (Map<String, Object>) summary.get("totalIncome");
        assertEquals(20000.0, ((Number) total.get("deltaAmount")).doubleValue());
        assertNotNull(out.get("incomeYearSeries"));
    }

    private static Map<String, BigDecimal> aggregate(double income) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("REAL_INCOME", BigDecimal.valueOf(income));
        return m;
    }
}
