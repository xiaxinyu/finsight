package com.finsight.application.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates monthly metrics from {@code v_transaction_finance_semantics} for a single user.
 */
@Repository
public class FinanceSemanticMetricsRepository {

    private static final String AGGREGATE_SQL = """
            select
              coalesce(sum(case when s.include_in_income_trend = 1 then s.amount else 0 end), 0) as real_income,
              coalesce(sum(case when s.economic_nature = 'refund' and s.cash_direction = 'inflow'
                  then s.amount else 0 end), 0) as refund_inflow,
              coalesce(sum(case when s.cash_direction = 'inflow'
                  and (s.category_l1_code = 'REIM' or s.category_code like 'REIM%') then s.amount else 0 end), 0)
                  as reimbursement_inflow,
              coalesce(sum(case when s.include_in_expense_trend = 1 then s.amount else 0 end), 0)
                  as consumption_expense,
              coalesce(sum(case when s.include_in_budget = 1 and s.budget_behavior = 'fixed'
                  then s.amount else 0 end), 0) as fixed_expense,
              coalesce(sum(case when s.include_in_expense_trend = 1 and s.budget_behavior = 'variable'
                  then s.amount else 0 end), 0) as variable_expense,
              coalesce(sum(case when s.include_in_expense_trend = 1
                  and s.budget_behavior not in ('fixed', 'essential', 'unclassified') then s.amount else 0 end), 0)
                  as discretionary_expense,
              coalesce(sum(case when s.include_in_expense_trend = 1 and s.budget_behavior = 'essential'
                  then s.amount else 0 end), 0) as essential_expense,
              coalesce(sum(case when s.economic_nature = 'investment' and s.cash_direction = 'inflow'
                  then s.amount else 0 end), 0) as investment_inflow,
              coalesce(sum(case when s.economic_nature = 'investment' and s.cash_direction = 'outflow'
                  then s.amount else 0 end), 0) as investment_outflow,
              coalesce(sum(case when s.economic_nature = 'liability' and s.cash_direction = 'inflow'
                  then s.amount else 0 end), 0) as liability_inflow,
              coalesce(sum(case when s.economic_nature = 'liability' and s.cash_direction = 'outflow'
                  then s.amount else 0 end), 0) as liability_repayment,
              coalesce(sum(case when s.economic_nature = 'transfer' and s.cash_direction = 'inflow'
                  then s.amount else 0 end), 0) as transfer_in,
              coalesce(sum(case when s.economic_nature = 'transfer' and s.cash_direction = 'outflow'
                  then s.amount else 0 end), 0) as transfer_out,
              coalesce(sum(case when s.category_l1_code = 'FEE' or s.category_code like 'FEE%'
                  then s.amount else 0 end), 0) as fee_expense,
              coalesce(sum(case when s.quality_state = 'unclassified' then s.amount else 0 end), 0)
                  as unclassified_amount,
              coalesce(sum(case when s.category_l1_code = 'OTHER' or s.category_code = 'OTHER'
                  or s.category_code like 'OTHER-%' then s.amount else 0 end), 0) as other_amount,
              count(*) as txn_count,
              coalesce(sum(case when s.quality_state = 'unclassified' then 1 else 0 end), 0)
                  as unclassified_count
            from v_transaction_finance_semantics s
            inner join transaction t on t.id = s.id
            where s.txn_date >= ? and s.txn_date <= ?
              and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))
            """;

    private final JdbcTemplate jdbcTemplate;

    public FinanceSemanticMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, BigDecimal> aggregateMonth(String userId, LocalDate start, LocalDate end) {
        return jdbcTemplate.query(AGGREGATE_SQL, rs -> {
            Map<String, BigDecimal> out = new LinkedHashMap<>();
            if (!rs.next()) {
                return out;
            }
            out.put("REAL_INCOME", bd(rs.getDouble("real_income")));
            out.put("REFUND_INFLOW", bd(rs.getDouble("refund_inflow")));
            out.put("REIMBURSEMENT_INFLOW", bd(rs.getDouble("reimbursement_inflow")));
            out.put("CONSUMPTION_EXPENSE", bd(rs.getDouble("consumption_expense")));
            out.put("FIXED_EXPENSE", bd(rs.getDouble("fixed_expense")));
            out.put("VARIABLE_EXPENSE", bd(rs.getDouble("variable_expense")));
            out.put("DISCRETIONARY_EXPENSE", bd(rs.getDouble("discretionary_expense")));
            out.put("ESSENTIAL_EXPENSE", bd(rs.getDouble("essential_expense")));
            out.put("INVESTMENT_INFLOW", bd(rs.getDouble("investment_inflow")));
            out.put("INVESTMENT_OUTFLOW", bd(rs.getDouble("investment_outflow")));
            out.put("LIABILITY_INFLOW", bd(rs.getDouble("liability_inflow")));
            out.put("LIABILITY_REPAYMENT", bd(rs.getDouble("liability_repayment")));
            out.put("TRANSFER_IN", bd(rs.getDouble("transfer_in")));
            out.put("TRANSFER_OUT", bd(rs.getDouble("transfer_out")));
            out.put("FEE_EXPENSE", bd(rs.getDouble("fee_expense")));
            out.put("UNCLASSIFIED_AMOUNT", bd(rs.getDouble("unclassified_amount")));
            out.put("OTHER_AMOUNT", bd(rs.getDouble("other_amount")));
            int txnCount = rs.getInt("txn_count");
            int uncls = rs.getInt("unclassified_count");
            double qualityScore = txnCount == 0 ? 100.0 : (100.0 * (txnCount - uncls) / txnCount);
            out.put("DATA_QUALITY_SCORE", bd(qualityScore));
            out.put("TRANSACTION_COUNT", bd(txnCount));
            out.put("UNCLASSIFIED_COUNT", bd(uncls));
            return out;
        }, start, end, userId, userId);
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static final String SUM_EXPENSE_BASE = """
            select coalesce(sum(s.amount), 0)
            from v_transaction_finance_semantics s
            inner join transaction t on t.id = s.id
            where s.include_in_expense_trend = 1
              and s.txn_date >= ? and s.txn_date <= ?
              and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))
            """;

    /** Total consumption expense in range (P&L expense trend). */
    public double sumConsumptionExpense(String userId, LocalDate start, LocalDate end) {
        Double v = jdbcTemplate.queryForObject(SUM_EXPENSE_BASE, Double.class, start, end, userId, userId);
        return v == null ? 0 : v;
    }

    /**
     * Budget actual for a bucket or category tree code using Reporting Classification rules.
     */
    public double sumBudgetActual(String userId, LocalDate start, LocalDate end,
                                  String categoryCode, String bucketKey) {
        if (categoryCode != null && !categoryCode.isBlank()) {
            String code = categoryCode.trim();
            Double v = jdbcTemplate.queryForObject(
                    SUM_EXPENSE_BASE + " and (s.category_code = ? or s.category_l1_code = ?)",
                    Double.class, start, end, userId, userId, code, code);
            return v == null ? 0 : v;
        }
        String bucket = bucketKey == null || bucketKey.isBlank() ? "all" : bucketKey.trim();
        String predicate = com.finsight.application.classification.BudgetSemanticBuckets.sqlPredicate(bucket);
        if (com.finsight.application.classification.BudgetSemanticBuckets.usesCategoryBind(bucket)) {
            Double v = jdbcTemplate.queryForObject(
                    SUM_EXPENSE_BASE + predicate,
                    Double.class, start, end, userId, userId, bucket, bucket);
            return v == null ? 0 : v;
        }
        Double v = jdbcTemplate.queryForObject(
                SUM_EXPENSE_BASE + predicate,
                Double.class, start, end, userId, userId);
        return v == null ? 0 : v;
    }

    public record SemanticTagYearAmount(String tagId, int year, double amount) {
    }

    /** Expense-trend totals grouped by calendar year and semantic_tag. */
    public java.util.List<SemanticTagYearAmount> sumExpenseBySemanticTagYears(
            String userId, int fromYear, int toYear) {
        return sumExpenseBySemanticTagYears(userId, fromYear, toYear, LocalDate.now());
    }

    /** Expense-trend totals grouped by calendar year and semantic_tag (YTD-safe for current year). */
    public java.util.List<SemanticTagYearAmount> sumExpenseBySemanticTagYears(
            String userId, int fromYear, int toYear, LocalDate asOf) {
        LocalDate start = LocalDate.of(fromYear, 1, 1);
        LocalDate endInc = AnalyticsDateRange.consumptionYearEndInclusive(toYear, asOf);
        return jdbcTemplate.query(
                """
                        select year(s.txn_date) as yr,
                               coalesce(nullif(trim(s.semantic_tag), ''), 'other') as tag_id,
                               coalesce(sum(s.amount), 0) as amount
                        from v_transaction_finance_semantics s
                        inner join transaction t on t.id = s.id
                        where s.include_in_expense_trend = 1
                          and s.txn_date >= ? and s.txn_date <= ?
                          and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))
                        group by year(s.txn_date), coalesce(nullif(trim(s.semantic_tag), ''), 'other')
                        order by yr, amount desc
                        """,
                (rs, rowNum) -> new SemanticTagYearAmount(
                        rs.getString("tag_id"),
                        rs.getInt("yr"),
                        rs.getDouble("amount")),
                start, endInc, userId, userId);
    }

    public record CategoryL1YearAmount(String l1Code, String l1Name, int year, double amount) {
    }

    /** Expense-trend totals grouped by calendar year and category L1 (YTD-safe for current year). */
    public java.util.List<CategoryL1YearAmount> sumExpenseByCategoryL1Years(
            String userId, int fromYear, int toYear, LocalDate asOf) {
        LocalDate start = LocalDate.of(fromYear, 1, 1);
        LocalDate endInc = AnalyticsDateRange.consumptionYearEndInclusive(toYear, asOf);
        return jdbcTemplate.query(
                """
                        select year(s.txn_date) as yr,
                               coalesce(nullif(trim(s.category_l1_code), ''), '__UNCLASSIFIED__') as l1_code,
                               coalesce(nullif(trim(s.category_l1_name), ''), 'Unclassified') as l1_name,
                               coalesce(sum(s.amount), 0) as amount
                        from v_transaction_finance_semantics s
                        inner join transaction t on t.id = s.id
                        where s.include_in_expense_trend = 1
                          and s.txn_date >= ? and s.txn_date <= ?
                          and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))
                        group by year(s.txn_date),
                                 coalesce(nullif(trim(s.category_l1_code), ''), '__UNCLASSIFIED__'),
                                 coalesce(nullif(trim(s.category_l1_name), ''), 'Unclassified')
                        order by yr, amount desc
                        """,
                (rs, rowNum) -> new CategoryL1YearAmount(
                        rs.getString("l1_code"),
                        rs.getString("l1_name"),
                        rs.getInt("yr"),
                        rs.getDouble("amount")),
                start, endInc, userId, userId);
    }

    /** Income-trend totals grouped by calendar year and semantic_tag (YTD-safe for current year). */
    public java.util.List<SemanticTagYearAmount> sumIncomeBySemanticTagYears(
            String userId, int fromYear, int toYear, LocalDate asOf) {
        LocalDate start = LocalDate.of(fromYear, 1, 1);
        LocalDate endInc = AnalyticsDateRange.consumptionYearEndInclusive(toYear, asOf);
        return jdbcTemplate.query(
                """
                        select year(s.txn_date) as yr,
                               coalesce(nullif(trim(s.semantic_tag), ''), 'other') as tag_id,
                               coalesce(sum(s.amount), 0) as amount
                        from v_transaction_finance_semantics s
                        inner join transaction t on t.id = s.id
                        where s.include_in_income_trend = 1
                          and s.txn_date >= ? and s.txn_date <= ?
                          and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))
                        group by year(s.txn_date), coalesce(nullif(trim(s.semantic_tag), ''), 'other')
                        order by yr, amount desc
                        """,
                (rs, rowNum) -> new SemanticTagYearAmount(
                        rs.getString("tag_id"),
                        rs.getInt("yr"),
                        rs.getDouble("amount")),
                start, endInc, userId, userId);
    }

    /** Income-trend totals grouped by calendar year and category L1 (YTD-safe for current year). */
    public java.util.List<CategoryL1YearAmount> sumIncomeByCategoryL1Years(
            String userId, int fromYear, int toYear, LocalDate asOf) {
        LocalDate start = LocalDate.of(fromYear, 1, 1);
        LocalDate endInc = AnalyticsDateRange.consumptionYearEndInclusive(toYear, asOf);
        return jdbcTemplate.query(
                """
                        select year(s.txn_date) as yr,
                               coalesce(nullif(trim(s.category_l1_code), ''), '__UNCLASSIFIED__') as l1_code,
                               coalesce(nullif(trim(s.category_l1_name), ''), 'Unclassified') as l1_name,
                               coalesce(sum(s.amount), 0) as amount
                        from v_transaction_finance_semantics s
                        inner join transaction t on t.id = s.id
                        where s.include_in_income_trend = 1
                          and s.txn_date >= ? and s.txn_date <= ?
                          and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))
                        group by year(s.txn_date),
                                 coalesce(nullif(trim(s.category_l1_code), ''), '__UNCLASSIFIED__'),
                                 coalesce(nullif(trim(s.category_l1_name), ''), 'Unclassified')
                        order by yr, amount desc
                        """,
                (rs, rowNum) -> new CategoryL1YearAmount(
                        rs.getString("l1_code"),
                        rs.getString("l1_name"),
                        rs.getInt("yr"),
                        rs.getDouble("amount")),
                start, endInc, userId, userId);
    }

    public record LiabilityYearFlow(int year, double borrowing, double repayment) {
        public double net() {
            return borrowing - repayment;
        }
    }

    public record LiabilityTagYearAmount(String tagId, int year, double amount) {
    }

    private static final String LIABILITY_BASE = """
            from v_transaction_finance_semantics s
            inner join transaction t on t.id = s.id
            where s.economic_nature = 'liability'
              and s.txn_date >= ? and s.txn_date <= ?
              and (t.created_by = ? or (? = '_anonymous' and t.created_by is null))
            """;

    /** Sum liability inflow (borrowing) or outflow (repayment) in an inclusive date range. */
    public double sumLiabilityFlow(String userId, LocalDate start, LocalDate endInc, String cashDirection) {
        Double v = jdbcTemplate.queryForObject(
                """
                        select coalesce(sum(s.amount), 0)
                        """
                        + LIABILITY_BASE
                        + " and s.cash_direction = ? ",
                Double.class,
                start, endInc, userId, userId, cashDirection);
        return v == null ? 0 : v;
    }

    /** Borrowing vs repayment totals grouped by calendar year (YTD-safe for current year). */
    public java.util.List<LiabilityYearFlow> sumLiabilityFlowByYear(
            String userId, int fromYear, int toYear, LocalDate asOf) {
        LocalDate start = LocalDate.of(fromYear, 1, 1);
        LocalDate endInc = AnalyticsDateRange.consumptionYearEndInclusive(toYear, asOf);
        return jdbcTemplate.query(
                """
                        select year(s.txn_date) as yr,
                               coalesce(sum(case when s.cash_direction = 'inflow' then s.amount else 0 end), 0)
                                   as borrowing,
                               coalesce(sum(case when s.cash_direction = 'outflow' then s.amount else 0 end), 0)
                                   as repayment
                        """
                        + LIABILITY_BASE
                        + """
                        group by year(s.txn_date)
                        order by yr
                        """,
                (rs, rowNum) -> new LiabilityYearFlow(
                        rs.getInt("yr"),
                        rs.getDouble("borrowing"),
                        rs.getDouble("repayment")),
                start, endInc, userId, userId);
    }

    /** Liability flows grouped by calendar year and semantic tag (inflow or outflow). */
    public java.util.List<LiabilityTagYearAmount> sumLiabilityBySemanticTagYears(
            String userId, int fromYear, int toYear, LocalDate asOf, String cashDirection) {
        LocalDate start = LocalDate.of(fromYear, 1, 1);
        LocalDate endInc = AnalyticsDateRange.consumptionYearEndInclusive(toYear, asOf);
        return jdbcTemplate.query(
                """
                        select year(s.txn_date) as yr,
                               coalesce(nullif(trim(s.semantic_tag), ''), 'liability') as tag_id,
                               coalesce(sum(s.amount), 0) as amount
                        """
                        + LIABILITY_BASE
                        + " and s.cash_direction = ? "
                        + """
                        group by year(s.txn_date),
                                 coalesce(nullif(trim(s.semantic_tag), ''), 'liability')
                        order by yr, amount desc
                        """,
                (rs, rowNum) -> new LiabilityTagYearAmount(
                        rs.getString("tag_id"),
                        rs.getInt("yr"),
                        rs.getDouble("amount")),
                start, endInc, userId, userId, cashDirection);
    }
}
