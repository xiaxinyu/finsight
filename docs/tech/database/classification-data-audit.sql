-- FinSight classification and report data audit.
-- Run against the application MySQL database before and after category/rule changes.
--
-- Workflow: docs/tech/database/classification-governance-workflow.zh-cn.md
-- Baseline template: docs/tech/database/classification-audit-baseline-template.md
--
--   mysql -u <user> -p finsight < docs/tech/database/classification-data-audit.sql > audit-$(date +%Y%m%d).txt
--
-- Manual remediation only (never auto-applied by Flyway):
--   orphan-rules-remediation.sql
--   invalid-rules-remediation.sql
--   transaction-category-field-remediation.sql
--   merchant-token-normalization.sql

-- 1. Category tree health.
select
    c.id,
    c.code,
    c.name,
    c.level,
    c.parent_id,
    p.code as parent_code,
    p.name as parent_name,
    c.txn_types,
    c.deleted
from cls_category c
left join cls_category p
    on p.code = c.parent_id or p.id = c.parent_id
order by c.level, c.parent_id, c.sort_no, c.code;

-- 2. Active categories without parent when level is child.
select
    c.id,
    c.code,
    c.name,
    c.level,
    c.parent_id
from cls_category c
left join cls_category p
    on p.code = c.parent_id or p.id = c.parent_id
where coalesce(c.deleted, 0) <> 1
  and coalesce(c.level, 1) > 1
  and p.id is null;

-- 3. Orphaned rules.
-- Remediation script: docs/tech/database/orphan-rules-remediation.sql
select
    r.id,
    r.pattern,
    r.pattern_type,
    r.category_id,
    r.priority,
    r.active,
    r.remark
from cls_rule r
left join cls_category c
    on c.code = r.category_id or c.id = r.category_id
where coalesce(r.category_id, '') <> ''
  and (c.id is null or coalesce(c.deleted, 0) = 1)
order by r.active desc, r.priority, r.pattern;

-- 4. Invalid rules with blank pattern.
-- Remediation script: docs/tech/database/invalid-rules-remediation.sql
select
    r.id,
    r.pattern,
    r.category_id,
    r.priority,
    r.active,
    r.remark
from cls_rule r
where r.pattern is null or trim(r.pattern) = ''
order by r.active desc, r.priority;

-- 5. Rules without category.
select
    r.id,
    r.pattern,
    r.pattern_type,
    r.category_id,
    r.priority,
    r.active,
    r.remark
from cls_rule r
where r.category_id is null or trim(r.category_id) = ''
order by r.active desc, r.priority;

-- 6. Duplicate active rule patterns across categories.
select
    lower(trim(r.pattern)) as normalized_pattern,
    count(*) as rule_count,
    group_concat(distinct r.category_id order by r.category_id separator ', ') as categories,
    group_concat(r.id order by r.priority separator ', ') as rule_ids
from cls_rule r
where coalesce(r.active, 1) = 1
  and r.pattern is not null
  and trim(r.pattern) <> ''
group by lower(trim(r.pattern))
having count(*) > 1
order by rule_count desc, normalized_pattern;

-- 7. Broad keyword risk.
select
    r.id,
    r.pattern,
    r.category_id,
    r.pattern_type,
    r.priority,
    r.active
from cls_rule r
where coalesce(r.active, 1) = 1
  and lower(trim(coalesce(r.pattern, ''))) in (
      '支付', '消费', '转账', '付款', '收款', '交易', '代扣', '快捷', '微信', '支付宝'
  )
order by r.priority, r.pattern;

-- 8. Rule count by category.
select
    c.code,
    c.name,
    count(r.id) as rule_count,
    sum(case when coalesce(r.active, 1) = 1 then 1 else 0 end) as active_rule_count
from cls_category c
left join cls_rule r
    on r.category_id = c.code or r.category_id = c.id
where coalesce(c.deleted, 0) <> 1
group by c.code, c.name
order by active_rule_count desc, rule_count desc, c.code;

-- 9. Transaction classification coverage.
select
    count(*) as total_txns,
    sum(case when coalesce(trim(consume_code), '') = '' and coalesce(trim(consume_name), '') = '' then 1 else 0 end) as unclassified_txns,
    round(sum(case when coalesce(trim(consume_code), '') = '' and coalesce(trim(consume_name), '') = '' then 1 else 0 end) / nullif(count(*), 0) * 100, 2) as unclassified_pct,
    round(sum(case when coalesce(trim(consume_code), '') = '' and coalesce(trim(consume_name), '') = '' then abs(coalesce(balance_money, 0) + coalesce(income_money, 0)) else 0 end), 2) as unclassified_amount
from `transaction`
where deleted is null or deleted = 0;

-- 10. Transactions pointing to missing or deleted categories.
select
    t.consume_code,
    t.consume_name,
    count(*) as txn_count,
    round(sum(abs(coalesce(t.balance_money, 0) + coalesce(t.income_money, 0))), 2) as amount
from `transaction` t
left join cls_category c
    on c.code = t.consume_code
where (t.deleted is null or t.deleted = 0)
  and coalesce(trim(t.consume_code), '') <> ''
  and (c.id is null or coalesce(c.deleted, 0) = 1)
group by t.consume_code, t.consume_name
order by amount desc;

-- 11. Transaction category field drift.
-- Remediation script: docs/tech/database/transaction-category-field-remediation.sql
-- Source of truth: consume_code; consume_name derived from cls_category.name.
select
    t.id,
    t.transaction_date,
    t.transaction_desc,
    t.consume_id,
    t.consume_code,
    t.consume_name,
    t.category_id,
    t.category_code,
    t.category_name,
    c.code as canonical_code,
    c.name as canonical_name
from `transaction` t
left join cls_category c
    on c.code = t.consume_code
   and coalesce(c.deleted, 0) = 0
where (t.deleted is null or t.deleted = 0)
  and coalesce(trim(t.consume_code), '') <> ''
  and c.id is not null
  and (
      coalesce(trim(t.consume_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.consume_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
      or coalesce(trim(t.category_code), '') <> coalesce(trim(t.consume_code), '')
      or coalesce(trim(t.category_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.category_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
  )
order by t.transaction_date desc
limit 200;

-- 12. Category amount by level for report sanity checks.
select
    v.category_l1_code,
    v.category_l1_name,
    v.category_code,
    v.category_name,
    v.direction,
    count(*) as txn_count,
    round(sum(v.amount), 2) as amount
from v_transaction_analytics v
where v.is_transfer = 0
  and v.is_refund = 0
group by v.category_l1_code, v.category_l1_name, v.category_code, v.category_name, v.direction
order by amount desc;

-- 13. Monthly income / expense sanity check from analytics view.
select
    date_format(v.txn_date, '%Y-%m') as year_month,
    round(sum(case when v.direction = 'income' and v.is_transfer = 0 then v.amount else 0 end), 2) as income,
    round(sum(case when v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 then v.amount else 0 end), 2) as expense,
    round(sum(case when v.direction = 'income' and v.is_transfer = 0 then v.amount else 0 end)
        - sum(case when v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0 then v.amount else 0 end), 2) as net
from v_transaction_analytics v
group by date_format(v.txn_date, '%Y-%m')
order by year_month desc;

-- 14. Merchant token coverage from analytics view (uses finsight_normalize_merchant_token).
select
    count(*) as expense_txns,
    sum(case when coalesce(trim(merchant_token), '') = '' then 1 else 0 end) as blank_merchant_token,
    round(sum(case when coalesce(trim(merchant_token), '') = '' then 1 else 0 end) / nullif(count(*), 0) * 100, 2) as blank_pct
from v_transaction_analytics
where direction = 'expense'
  and is_transfer = 0
  and is_refund = 0;

-- 15. Merchant profile tokens that may not match analytics token directly.
-- Rows here need investigation because report drilldown may miss transactions.
select
    mp.user_id,
    mp.merchant_token,
    mp.display_name,
    mp.txn_count as profile_txn_count,
    count(v.id) as analytics_matching_txns
from fin_merchant_profile mp
left join v_transaction_analytics v
    on v.merchant_token = mp.merchant_token
   and v.direction = 'expense'
   and v.is_transfer = 0
   and v.is_refund = 0
group by mp.user_id, mp.merchant_token, mp.display_name, mp.txn_count
having analytics_matching_txns = 0
order by mp.txn_count desc;

-- 16. Fixed vs variable sanity.
select
    v.fixed_flag,
    v.category_l1_code,
    v.category_l1_name,
    count(*) as txn_count,
    round(sum(v.amount), 2) as amount
from v_transaction_analytics v
where v.direction = 'expense'
  and v.is_transfer = 0
  and v.is_refund = 0
group by v.fixed_flag, v.category_l1_code, v.category_l1_name
order by v.fixed_flag desc, amount desc;

-- 17. Transfer and refund exclusion volume.
select
    date_format(v.txn_date, '%Y-%m') as year_month,
    sum(case when v.is_transfer = 1 then 1 else 0 end) as transfer_txns,
    round(sum(case when v.is_transfer = 1 then v.amount else 0 end), 2) as transfer_amount,
    sum(case when v.is_refund = 1 then 1 else 0 end) as refund_txns,
    round(sum(case when v.is_refund = 1 then v.amount else 0 end), 2) as refund_amount
from v_transaction_analytics v
group by date_format(v.txn_date, '%Y-%m')
order by year_month desc;

-- 18. Top unclassified raw descriptions for rule creation.
select
    coalesce(nullif(trim(opponent_name), ''), nullif(trim(transaction_desc), ''), nullif(trim(memo), ''), 'UNKNOWN') as raw_text,
    count(*) as txn_count,
    round(sum(abs(coalesce(balance_money, 0) + coalesce(income_money, 0))), 2) as amount
from `transaction`
where (deleted is null or deleted = 0)
  and coalesce(trim(consume_code), '') = ''
  and coalesce(trim(consume_name), '') = ''
group by raw_text
order by txn_count desc, amount desc
limit 100;

-- 19. Top transactions in OTHER / catch-all categories (by amount).
-- Export as baseline-other-consumption-top100.csv
select
    t.id,
    t.transaction_date,
    t.consume_code,
    t.consume_name,
    coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), 'UNKNOWN') as raw_text,
    round(abs(coalesce(t.expense_amount, 0)) + abs(coalesce(t.income_money, 0)), 2) as amount
from `transaction` t
left join cls_category c on c.code = t.consume_code
where (t.deleted is null or t.deleted = 0)
  and coalesce(trim(t.consume_code), '') <> ''
  and (
      t.consume_code like 'OTHER%'
      or coalesce(c.name, t.consume_name, '') like '%其它%'
      or coalesce(c.name, t.consume_name, '') like '%无法归类%'
  )
order by amount desc, t.transaction_date desc
limit 100;

-- 20. Audit baseline summary counts (save before/after each governance sprint).
-- Export as baseline-summary.json or paste into classification-audit-baseline-template.md
select
    'BASELINE_SUMMARY' as artifact,
    (select count(*) from cls_rule r
        left join cls_category c on c.code = r.category_id or c.id = r.category_id
        where coalesce(r.category_id, '') <> ''
          and (c.id is null or coalesce(c.deleted, 0) = 1)
          and coalesce(r.active, 1) = 1) as active_orphan_rules,
    (select count(*) from cls_rule r
        where (r.pattern is null or trim(r.pattern) = '')
          and coalesce(r.active, 1) = 1) as active_invalid_pattern_rules,
    (select count(*) from `transaction` t
        inner join cls_category c on c.code = t.consume_code and coalesce(c.deleted, 0) = 0
        where coalesce(t.deleted, 0) = 0
          and coalesce(trim(t.consume_code), '') <> ''
          and (
              coalesce(trim(t.consume_name), '') <> coalesce(trim(c.name), '')
              or coalesce(trim(t.category_code), '') <> coalesce(trim(t.consume_code), '')
          )) as category_field_drift_rows,
    (select sum(case when coalesce(trim(consume_code), '') = '' and coalesce(trim(consume_name), '') = ''
        then 1 else 0 end) from `transaction` where coalesce(deleted, 0) = 0) as unclassified_txns,
    (select count(*) from `transaction` t
        where coalesce(t.deleted, 0) = 0
          and coalesce(trim(t.consume_code), '') <> ''
          and (t.consume_code like 'OTHER%')) as other_category_txns,
    (select count(*) from (
        select mp.merchant_token
        from fin_merchant_profile mp
        left join v_transaction_analytics v
            on v.merchant_token = mp.merchant_token
           and v.direction = 'expense' and v.is_transfer = 0 and v.is_refund = 0
        group by mp.user_id, mp.merchant_token, mp.display_name, mp.txn_count
        having count(v.id) = 0
    ) merchant_mismatch) as merchant_profile_mismatch_count;

-- 21. Merchant token normalization samples (investigation rows for drilldown parity).
-- Export as baseline-merchant-token-samples.csv
-- Remediation reference: docs/tech/database/merchant-token-normalization.sql
select
    t.id,
    t.transaction_date,
    coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), '') as raw_merchant,
    finsight_normalize_merchant_token(
        coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), '')) as normalized_token,
    v.merchant_token as analytics_view_token,
    t.consume_code,
    round(abs(coalesce(t.expense_amount, 0)), 2) as amount
from `transaction` t
left join v_transaction_analytics v on v.id = t.id
where coalesce(t.deleted, 0) = 0
  and abs(coalesce(t.expense_amount, 0)) > 0
  and (
      coalesce(trim(v.merchant_token), '') = ''
      or finsight_normalize_merchant_token(
          coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), ''))
         <> coalesce(v.merchant_token, '')
  )
order by t.transaction_date desc
limit 100;

