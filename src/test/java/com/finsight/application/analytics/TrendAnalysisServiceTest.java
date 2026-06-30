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
        when(semanticMetricsRepository.aggregateMonth(eq("user1"), eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2025, 12, 31))))
                .thenReturn(aggregate(10000, 6000, 1200));
        when(semanticMetricsRepository.aggregateMonth(eq("user1"), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(aggregate(12000, 9000, 1500));
        when(semanticMetricsRepository.sumExpenseBySemanticTagYears("user1", 2025, 2026))
                .thenReturn(List.of(
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("dining_spending", 2025, 1000),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("transport_spending", 2025, 500),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("dining_spending", 2026, 1800),
                        new FinanceSemanticMetricsRepository.SemanticTagYearAmount("transport_spending", 2026, 900)));
        when(jdbcTemplate.queryForList(contains("v.txn_date >="),
                eq(LocalDate.of(2025, 1, 1)), eq(LocalDate.of(2026, 1, 1)), eq("user1"), eq("user1")))
                .thenReturn(List.of(Map.of(
                        "opponent_name", "Uber",
                        "transaction_desc", "",
                        "amount", 200)));
        when(jdbcTemplate.queryForList(contains("v.txn_date >="),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2027, 1, 1)), eq("user1"), eq("user1")))
                .thenReturn(List.of(Map.of(
                        "opponent_name", "Uber",
                        "transaction_desc", "",
                        "amount", 500)));

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
    }

    private static Map<String, BigDecimal> aggregate(double income, double expense, double fixed) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("REAL_INCOME", BigDecimal.valueOf(income));
        m.put("CONSUMPTION_EXPENSE", BigDecimal.valueOf(expense));
        m.put("FIXED_EXPENSE", BigDecimal.valueOf(fixed));
        return m;
    }
}
