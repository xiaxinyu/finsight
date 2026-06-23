-- §20 Audit baseline summary counts — export baseline-summary.tsv (convert to JSON in script)
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
