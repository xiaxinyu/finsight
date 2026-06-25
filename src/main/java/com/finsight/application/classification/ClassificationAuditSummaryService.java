package com.finsight.application.classification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Loads {@link ClassificationAuditSummary} from live DB for remediation planning.
 * Does not mutate data — exports are manual via {@code scripts/db/export-classification-audit-baseline.sh}.
 */
@Service
public class ClassificationAuditSummaryService {

    private final JdbcTemplate jdbcTemplate;

    public ClassificationAuditSummaryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ClassificationAuditSummary loadSummary() {
        if (!tableExists("transaction") || !tableExists("cls_rule")) {
            return ClassificationAuditSummary.empty();
        }
        return new ClassificationAuditSummary(
                longVal(sqlActiveOrphanRules()),
                longVal(sqlActiveInvalidPatterns()),
                longVal(sqlCategoryFieldDrift()),
                longVal(sqlUnclassifiedTxns()),
                longVal(sqlOtherCategoryTxns()),
                tableExists("fin_merchant_profile") && viewExists("v_transaction_finance_semantics")
                        ? longVal(sqlMerchantProfileMismatch()) : 0L,
                longVal(sqlDuplicatePatternGroups()),
                longVal(sqlBroadKeywordRules()),
                longVal(sqlRulesWithoutCategory()),
                longVal(sqlTxnMissingCategoryGroups()));
    }

    private Long longVal(String sql) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class);
        return v == null ? 0L : v;
    }

    private boolean tableExists(String table) {
        Integer n = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = ?",
                Integer.class,
                table);
        return n != null && n > 0;
    }

    private boolean viewExists(String view) {
        Integer n = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = ? and table_type = 'VIEW'",
                Integer.class,
                view);
        return n != null && n > 0;
    }

    static String sqlActiveOrphanRules() {
        return "select count(*) from cls_rule r "
                + "left join cls_category c on c.code = r.category_id or c.id = r.category_id "
                + "where coalesce(r.category_id, '') <> '' "
                + "and (c.id is null or coalesce(c.deleted, 0) = 1) "
                + "and coalesce(r.active, 1) = 1";
    }

    static String sqlActiveInvalidPatterns() {
        return "select count(*) from cls_rule r "
                + "where (r.pattern is null or trim(r.pattern) = '') "
                + "and coalesce(r.active, 1) = 1";
    }

    static String sqlCategoryFieldDrift() {
        return "select count(*) from `transaction` t "
                + "inner join cls_category c on c.code = t.consume_code and coalesce(c.deleted, 0) = 0 "
                + "where coalesce(t.deleted, 0) = 0 "
                + "and coalesce(trim(t.consume_code), '') <> '' "
                + "and (coalesce(trim(t.consume_name), '') <> coalesce(trim(c.name), '') "
                + "     or coalesce(trim(t.category_code), '') <> coalesce(trim(t.consume_code), ''))";
    }

    static String sqlUnclassifiedTxns() {
        return "select count(*) from `transaction` "
                + "where coalesce(deleted, 0) = 0 "
                + "and coalesce(trim(consume_code), '') = '' "
                + "and coalesce(trim(consume_name), '') = ''";
    }

    static String sqlOtherCategoryTxns() {
        return "select count(*) from `transaction` t "
                + "where coalesce(t.deleted, 0) = 0 "
                + "and coalesce(trim(t.consume_code), '') <> '' "
                + "and t.consume_code like 'OTHER%'";
    }

    static String sqlMerchantProfileMismatch() {
        return "select count(*) from ( "
                + "select mp.merchant_token from fin_merchant_profile mp "
                + "left join v_transaction_finance_semantics v on v.merchant_token = mp.merchant_token "
                + "  and v.include_in_expense_trend = 1 "
                + "group by mp.user_id, mp.merchant_token, mp.display_name, mp.txn_count "
                + "having count(v.id) = 0) x";
    }

    static String sqlDuplicatePatternGroups() {
        return "select count(*) from ( "
                + "select lower(trim(r.pattern)) as p from cls_rule r "
                + "where coalesce(r.active, 1) = 1 and r.pattern is not null and trim(r.pattern) <> '' "
                + "group by lower(trim(r.pattern)) having count(*) > 1) x";
    }

    static String sqlBroadKeywordRules() {
        return "select count(*) from cls_rule r "
                + "where coalesce(r.active, 1) = 1 "
                + "and lower(trim(coalesce(r.pattern, ''))) in ("
                + "'支付','消费','转账','付款','收款','交易','代扣','快捷','微信','支付宝')";
    }

    static String sqlRulesWithoutCategory() {
        return "select count(*) from cls_rule r "
                + "where r.category_id is null or trim(r.category_id) = ''";
    }

    static String sqlTxnMissingCategoryGroups() {
        return "select count(*) from ( "
                + "select t.consume_code from `transaction` t "
                + "left join cls_category c on c.code = t.consume_code "
                + "where coalesce(t.deleted, 0) = 0 "
                + "and coalesce(trim(t.consume_code), '') <> '' "
                + "and (c.id is null or coalesce(c.deleted, 0) = 1) "
                + "group by t.consume_code, t.consume_name) x";
    }
}
