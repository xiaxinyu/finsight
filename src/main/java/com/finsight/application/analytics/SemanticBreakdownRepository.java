package com.finsight.application.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SemanticBreakdownRepository {

    private static final String BY_TAG_BASE = """
            SELECT COALESCE(NULLIF(TRIM(s.semantic_tag), ''), 'other') AS tag_id,
                   COALESCE(SUM(s.amount), 0) AS amount
            FROM v_transaction_finance_semantics s
            INNER JOIN transaction t ON t.id = s.id
            LEFT JOIN statement st ON st.id = t.statement_id AND (st.deleted IS NULL OR st.deleted != 1)
            WHERE s.txn_date >= ? AND s.txn_date <= ?
              AND (t.created_by = ? OR (? = '_anonymous' AND t.created_by IS NULL))
            """;

    private final JdbcTemplate jdbcTemplate;

    public SemanticBreakdownRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TagAmountRow> bySemanticTag(SemanticBreakdownQuery query) {
        StringBuilder sql = new StringBuilder(BY_TAG_BASE);
        List<Object> args = new ArrayList<>();
        args.add(query.start());
        args.add(query.end());
        args.add(query.userId());
        args.add(query.userId());

        appendScopeFilter(sql, query.scope());

        if (StringUtils.hasText(query.cardId())) {
            sql.append(
                    """
                             AND (
                                 t.bank_card_id = ?
                                 OR (
                                     (t.bank_card_id IS NULL OR TRIM(t.bank_card_id) = '')
                                     AND EXISTS (
                                         SELECT 1 FROM fin_bank_account fc
                                         WHERE fc.id = ?
                                           AND (fc.deleted IS NULL OR fc.deleted != 1)
                                           AND UPPER(TRIM(COALESCE(st.source_bank_code, ''))) = UPPER(TRIM(COALESCE(fc.bank_code, '')))
                                     )
                                 )
                             )
                            """);
            args.add(query.cardId().trim());
            args.add(query.cardId().trim());
        }
        if (StringUtils.hasText(query.consumeId())) {
            sql.append(" AND (t.consume_id = ? OR t.category_id = ?)");
            String consumeId = query.consumeId().trim();
            args.add(consumeId);
            args.add(consumeId);
        }

        sql.append("""
                 GROUP BY COALESCE(NULLIF(TRIM(s.semantic_tag), ''), 'other')
                 ORDER BY amount DESC
                """);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new TagAmountRow(
                rs.getString("tag_id"),
                rs.getDouble("amount")), args.toArray());
    }

    /** @deprecated use {@link #bySemanticTag(SemanticBreakdownQuery)} */
    @Deprecated
    public List<TagAmountRow> expenseBySemanticTag(SemanticBreakdownQuery query) {
        return bySemanticTag(new SemanticBreakdownQuery(
                query.userId(), query.start(), query.end(), query.cardId(), query.consumeId(), BreakdownScope.EXPENSE));
    }

    private static void appendScopeFilter(StringBuilder sql, BreakdownScope scope) {
        if (scope == null || scope == BreakdownScope.EXPENSE) {
            sql.append(" AND s.include_in_expense_trend = 1");
            return;
        }
        switch (scope) {
            case INCOME -> sql.append(" AND s.include_in_income_trend = 1");
            case NON_PNL -> sql.append("""
                     AND COALESCE(NULLIF(TRIM(s.semantic_tag), ''), 'other') IN (
                         'transfer', 'finance_loan', 'finance_credit_loan', 'finance_installment',
                         'investment', 'asset_adjustment', 'liability'
                     )
                    """);
            case TAX -> sql.append("""
                     AND COALESCE(NULLIF(TRIM(s.semantic_tag), ''), 'other') IN ('tax_expense', 'tax_refund')
                    """);
            case REFUND -> sql.append("""
                     AND COALESCE(NULLIF(TRIM(s.semantic_tag), ''), 'other') = 'refund_reimbursement'
                    """);
            case ALL -> { /* no inclusion filter */ }
            default -> sql.append(" AND s.include_in_expense_trend = 1");
        }
    }

    public enum BreakdownScope {
        EXPENSE, INCOME, NON_PNL, TAX, REFUND, ALL
    }

    public record SemanticBreakdownQuery(
            String userId,
            LocalDate start,
            LocalDate end,
            String cardId,
            String consumeId,
            BreakdownScope scope) {

        public SemanticBreakdownQuery(String userId, LocalDate start, LocalDate end, String cardId, String consumeId) {
            this(userId, start, end, cardId, consumeId, BreakdownScope.EXPENSE);
        }
    }

    public record TagAmountRow(String tagId, double amount) {
    }
}
