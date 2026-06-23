package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryStatsSupportTest {

    @Test
    void countSqlUsesTransactionMatch() {
        String sql = CategoryStatsSupport.countTransactionsSql(List.of("INC-01"));
        assertTrue(sql.contains("t.consume_code = ?"));
        assertTrue(sql.contains("count(*)"));
    }

    @Test
    void lastDateSqlUsesMaxTransactionDate() {
        String sql = CategoryStatsSupport.lastTransactionDateSql(List.of("DAILY-01"));
        assertTrue(sql.contains("max(t.transaction_date)"));
        assertFalse(sql.contains("order by year_month"));
    }
}
