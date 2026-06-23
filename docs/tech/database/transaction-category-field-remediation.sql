-- FinSight transaction category field drift remediation (manual execution only).
-- Source of truth: consume_code; consume_name from cls_category.name.
-- Does NOT modify cls_category.code — only transaction category columns.
--
--   mysql -u <user> -p finsight < docs/tech/database/transaction-category-field-remediation.sql
--
-- MySQL Workbench "safe update mode" (Error 1175): UPDATEs use transaction.id (PK).
--
-- Workflow:
--   1. Run BEFORE inventory (Step 0) and save results.
--   2. Review drift rows; note any consume_code pointing at deleted/missing categories (audit #10).
--   3. Run Step 2 sync for rows with valid active consume_code.
--   4. Run AFTER verification (Step 3) — drift count should be 0.

-- =============================================================================
-- Step 0: BEFORE — field drift inventory (audit #11)
-- =============================================================================
select
    'BEFORE' as phase,
    count(*) as drift_row_count
from `transaction` t
inner join cls_category c
    on c.code = t.consume_code
   and coalesce(c.deleted, 0) = 0
where (t.deleted is null or t.deleted = 0)
  and coalesce(trim(t.consume_code), '') <> ''
  and (
      coalesce(trim(t.consume_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.consume_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
      or coalesce(trim(t.category_code), '') <> coalesce(trim(t.consume_code), '')
      or coalesce(trim(t.category_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.category_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
  );

select
    'BEFORE' as phase,
    t.id,
    t.transaction_date,
    t.consume_id,
    t.consume_code,
    t.consume_name,
    t.category_id,
    t.category_code,
    t.category_name,
    c.code as canonical_code,
    c.name as canonical_name
from `transaction` t
inner join cls_category c
    on c.code = t.consume_code
   and coalesce(c.deleted, 0) = 0
where (t.deleted is null or t.deleted = 0)
  and coalesce(trim(t.consume_code), '') <> ''
  and (
      coalesce(trim(t.consume_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.consume_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
      or coalesce(trim(t.category_code), '') <> coalesce(trim(t.consume_code), '')
      or coalesce(trim(t.category_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.category_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
  )
order by t.transaction_date desc
limit 200;

-- =============================================================================
-- Processing decision log (fill in after review)
-- =============================================================================
-- | txn_id | consume_code | decision | reason |
-- |--------|--------------|----------|--------|
-- | (paste)| DAILY-01     | sync     | name/id drift only |
-- | (paste)| OLD-CODE     | skip     | category deleted — fix code first |

-- =============================================================================
-- Step 2: Sync drift rows from consume_code + cls_category (active categories only)
-- =============================================================================
update `transaction` t
inner join cls_category c
    on c.code = t.consume_code
   and coalesce(c.deleted, 0) = 0
set
    t.consume_id = c.code,
    t.category_id = c.code,
    t.category_code = t.consume_code,
    t.consume_name = c.name,
    t.category_name = c.name
where t.id in (
    select tid from (
        select t2.id as tid
        from `transaction` t2
        inner join cls_category c2
            on c2.code = t2.consume_code
           and coalesce(c2.deleted, 0) = 0
        where (t2.deleted is null or t2.deleted = 0)
          and coalesce(trim(t2.consume_code), '') <> ''
          and (
              coalesce(trim(t2.consume_name), '') <> coalesce(trim(c2.name), '')
              or coalesce(trim(t2.consume_id), '') not in ('', coalesce(trim(c2.code), ''), coalesce(trim(c2.id), ''))
              or coalesce(trim(t2.category_code), '') <> coalesce(trim(t2.consume_code), '')
              or coalesce(trim(t2.category_name), '') <> coalesce(trim(c2.name), '')
              or coalesce(trim(t2.category_id), '') not in ('', coalesce(trim(c2.code), ''), coalesce(trim(c2.id), ''))
          )
    ) drift_txn_ids
);

-- =============================================================================
-- Step 3: AFTER — verification (drift count should be 0)
-- =============================================================================
select
    count(*) as drift_row_count
from `transaction` t
inner join cls_category c
    on c.code = t.consume_code
   and coalesce(c.deleted, 0) = 0
where (t.deleted is null or t.deleted = 0)
  and coalesce(trim(t.consume_code), '') <> ''
  and (
      coalesce(trim(t.consume_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.consume_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
      or coalesce(trim(t.category_code), '') <> coalesce(trim(t.consume_code), '')
      or coalesce(trim(t.category_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.category_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
  );

select
    'AFTER' as phase,
    t.id,
    t.consume_code,
    t.consume_name,
    t.category_code,
    t.category_name
from `transaction` t
inner join cls_category c
    on c.code = t.consume_code
   and coalesce(c.deleted, 0) = 0
where (t.deleted is null or t.deleted = 0)
  and coalesce(trim(t.consume_code), '') <> ''
  and (
      coalesce(trim(t.consume_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.consume_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
      or coalesce(trim(t.category_code), '') <> coalesce(trim(t.consume_code), '')
      or coalesce(trim(t.category_name), '') <> coalesce(trim(c.name), '')
      or coalesce(trim(t.category_id), '') not in ('', coalesce(trim(c.code), ''), coalesce(trim(c.id), ''))
  )
order by t.transaction_date desc
limit 50;
