package com.finsight.application.classification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryImpactSupportSqlTest {

    @Test
    void monthlyAmountSqlAvoidsYearMonthOrderByAlias() {
        String sql = CategoryImpactSupport.monthlyAmountSql(List.of("INC-01"), 24);
        assertTrue(sql.contains("as txn_month"));
        assertTrue(sql.contains("order by date_format(t.transaction_date, '%Y-%m')"));
        assertFalse(sql.contains("order by year_month"));
    }

    @Test
    void transactionMatchSqlUsesTableAlias() {
        String sql = CategoryImpactSupport.transactionMatchSql(List.of("INC-01"));
        assertTrue(sql.contains("t.consume_code = ?"));
    }
}
