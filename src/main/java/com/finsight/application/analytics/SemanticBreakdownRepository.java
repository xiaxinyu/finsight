package com.finsight.application.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SemanticBreakdownRepository {

    private static final String EXPENSE_BY_TAG_SQL = """
            SELECT COALESCE(NULLIF(TRIM(s.semantic_tag), ''), 'other') AS tag_id,
                   COALESCE(SUM(s.amount), 0) AS amount
            FROM v_transaction_finance_semantics s
            INNER JOIN transaction t ON t.id = s.id
            WHERE s.include_in_expense_trend = 1
              AND s.txn_date >= ? AND s.txn_date <= ?
              AND (t.created_by = ? OR (? = '_anonymous' AND t.created_by IS NULL))
            GROUP BY COALESCE(NULLIF(TRIM(s.semantic_tag), ''), 'other')
            ORDER BY amount DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public SemanticBreakdownRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TagAmountRow> expenseBySemanticTag(String userId, LocalDate start, LocalDate end) {
        return jdbcTemplate.query(EXPENSE_BY_TAG_SQL, (rs, rowNum) -> new TagAmountRow(
                rs.getString("tag_id"),
                rs.getDouble("amount")), start, end, userId, userId);
    }

    public record TagAmountRow(String tagId, double amount) {
    }
}
