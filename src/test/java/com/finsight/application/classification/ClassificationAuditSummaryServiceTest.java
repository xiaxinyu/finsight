package com.finsight.application.classification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationAuditSummaryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ClassificationAuditSummaryService service;

    @BeforeEach
    void setUp() {
        service = new ClassificationAuditSummaryService(jdbcTemplate);
    }

    @Test
    void loadSummaryAggregatesCounts() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("transaction")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("cls_rule")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("fin_merchant_profile")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("v_transaction_analytics")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(3L, 1L, 0L, 12L, 8L, 2L, 4L, 1L, 0L, 1L);

        ClassificationAuditSummary summary = service.loadSummary();

        assertEquals(3, summary.activeOrphanRules());
        assertEquals(12, summary.unclassifiedTxns());
        assertEquals(4, summary.duplicatePatternGroups());
    }

    @Test
    void loadSummaryReturnsEmptyWhenCoreTablesMissing() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("transaction")))
                .thenReturn(0);

        ClassificationAuditSummary summary = service.loadSummary();

        assertEquals(0, summary.activeOrphanRules());
        assertEquals(0, summary.unclassifiedTxns());
    }
}
