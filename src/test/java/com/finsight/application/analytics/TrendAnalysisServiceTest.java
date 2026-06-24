package com.finsight.application.analytics;

import com.finsight.application.authentication.AuthenticationFacade;
import com.finsight.application.query.TransactionQuerySupport;
import com.finsight.domain.model.CategoryAggregate;
import com.finsight.domain.model.KeyValue;
import com.finsight.domain.port.TransactionRepository;
import com.finsight.infrastructure.mapper.FinancialMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class TrendAnalysisServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionQuerySupport querySupport;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private FinancialMapper financialMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private TrendAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new TrendAnalysisService(
                transactionRepository, querySupport, authenticationFacade, financialMapper, jdbcTemplate);
        when(authenticationFacade.getUserName()).thenReturn("user1");
    }

    @Test
    void trends_returnsDecomposedYoYWithMovers() throws Exception {
        when(transactionRepository.monthIncomeReport(any()))
                .thenReturn(List.of(kv(5000), kv(5000)))
                .thenReturn(List.of(kv(6000), kv(6000)));
        when(transactionRepository.monthExpenseReport(any()))
                .thenReturn(List.of(kv(3000), kv(3000)))
                .thenReturn(List.of(kv(4500), kv(4500)));
        when(financialMapper.sumFixedBucketYear(2025)).thenReturn(1200.0);
        when(financialMapper.sumFixedBucketYear(2026)).thenReturn(1500.0);
        when(transactionRepository.consumeReport(any())).thenReturn(
                List.of(cat("FOOD", "Food", 1000), cat("TRAVEL", "Travel", 500)),
                List.of(cat("FOOD", "Food", 1800), cat("TRAVEL", "Travel", 900)));
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
    }

    private static KeyValue kv(double value) {
        KeyValue kv = new KeyValue();
        kv.setValue(String.valueOf(value));
        return kv;
    }

    private static CategoryAggregate cat(String code, String name, double value) {
        CategoryAggregate cat = new CategoryAggregate();
        cat.setCode(code);
        cat.setName(name);
        cat.setValue(value);
        return cat;
    }
}
