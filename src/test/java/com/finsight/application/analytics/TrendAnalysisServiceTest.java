package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendAnalysisServiceTest {

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private FinanceSemanticMetricsRepository semanticMetricsRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private TrendAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new TrendAnalysisService(authenticationFacade, semanticMetricsRepository, jdbcTemplate);
        when(authenticationFacade.getUserName()).thenReturn("user1");
    }

    @Test
    void trends_returnsDecomposedYoYWithMovers() throws Exception {
        when(semanticMetricsRepository.aggregateMonth(eq("user1"), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(1);
                    if (start.getYear() <= 2025) {
                        return aggregate(10000, 6000, 1200);
                    }
                    return aggregate(12000, 9000, 1500);
                });
        when(semanticMetricsRepository.sumExpenseBySemanticTagYears(eq("user1"), eq(2025), eq(2026), any()))
                .thenReturn(List.of(
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("dining_spending", 2025, 1000),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("transport_spending", 2025, 500),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("dining_spending", 2026, 1800),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("transport_spending", 2026, 900)));
        when(semanticMetricsRepository.sumExpenseByCategoryL1Years(eq("user1"), eq(2025), eq(2026), any()))
                .thenReturn(List.of(
                        new FinanceSemanticMetricsRepository.CategoryL1YearAmount("FOOD", "Food", 2025, 1500),
                        new FinanceSemanticMetricsRepository.CategoryL1YearAmount("FOOD", "Food", 2026, 2700)));
        when(jdbcTemplate.queryForList(anyString(), any(LocalDate.class), any(LocalDate.class), eq("user1"), eq("user1")))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(1);
                    double amount = start.getYear() <= 2025 ? 200.0 : 500.0;
                    return List.of(Map.of(
                            "opponent_name", "Uber",
                            "transaction_desc", "",
                            "amount", amount));
                });

        Map<String, Object> out = service.trends(2025, 2026);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) out.get("summary");
        assertNotNull(summary.get("headline"));
        @SuppressWarnings("unchecked")
        Map<String, Object> expense = (Map<String, Object>) summary.get("expense");
        assertEquals(3000.0, ((Number) expense.get("deltaAmount")).doubleValue());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trends = (List<Map<String, Object>>) out.get("trends");
        assertTrue(trends.stream().anyMatch(t -> "expense_yoy".equals(t.get("type"))));
        assertTrue(trends.stream().anyMatch(t -> "category_mover".equals(t.get("type"))));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> merchants = (List<Map<String, Object>>) out.get("topMerchantMovers");
        assertEquals(1, merchants.size());
        assertNotNull(merchants.get(0).get("drillDown"));
        @SuppressWarnings("unchecked")
        Map<String, String> drill = (Map<String, String>) merchants.get(0).get("drillDown");
        assertNotNull(drill.get("merchantToken"));
        assertNull(drill.get("demoArea"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categories = (List<Map<String, Object>>) out.get("topCategoryGrowth");
        assertTrue(categories.stream().anyMatch(c -> "dining_spending".equals(c.get("categoryCode"))));
        assertEquals("v_transaction_finance_semantics.semantic_tag", out.get("metricsSource"));

        @SuppressWarnings("unchecked")
        Map<String, Object> matrix = (Map<String, Object>) out.get("categoryYearMatrix");
        assertNotNull(matrix);
        @SuppressWarnings("unchecked")
        List<Integer> years = (List<Integer>) matrix.get("years");
        assertEquals(2, years.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matrixRows = (List<Map<String, Object>>) matrix.get("rows");
        assertTrue(matrixRows.stream().anyMatch(r -> "dining_spending".equals(r.get("tagId"))));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> series = (List<Map<String, Object>>) out.get("consumptionYearSeries");
        assertNotNull(series);
        assertEquals(2, series.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> l1Matrix = (Map<String, Object>) out.get("categoryL1YearMatrix");
        assertNotNull(l1Matrix);
    }

    private static Map<String, BigDecimal> aggregate(double income, double expense, double fixed) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("REAL_INCOME", BigDecimal.valueOf(income));
        m.put("CONSUMPTION_EXPENSE", BigDecimal.valueOf(expense));
        m.put("FIXED_EXPENSE", BigDecimal.valueOf(fixed));
        return m;
    }
}
