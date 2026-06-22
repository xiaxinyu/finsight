-- FinSight invalid / legacy rule remediation (manual execution only).
-- Targets audit §4 (blank pattern) and inactive rows without archive remark.
-- Does NOT modify cls_category.code — only cls_rule.pattern / category_id / active / remark.
--
--   mysql -u <user> -p finsight < docs/tech/database/invalid-rules-remediation.sql
--
-- MySQL Workbench "safe update mode" (Error 1175): UPDATEs use cls_rule.id (PK).
--
-- Workflow:
--   1. Run BEFORE inventory (Step 0) and save results.
--   2. Review Step 2 fix candidates; apply Step 3 manual fixes where a keyword can be restored.
--   3. Run Step 4 archive for remaining active blank-pattern rules.
--   4. Run Step 5 backfill remark on inactive blank rows still missing a marker.
--   5. Run AFTER verification (Step 6) — active invalid count should be 0.

-- =============================================================================
-- Step 0: BEFORE — invalid rule inventory (audit §4)
-- =============================================================================
select
    'BEFORE' as phase,
    r.id,
    r.pattern,
    r.pattern_type,
    r.category_id,
    r.priority,
    r.active,
    r.remark,
    case
        when r.pattern is null or trim(r.pattern) = '' then 'blank_pattern'
        else 'other'
    end as issue_type
from cls_rule r
where r.pattern is null or trim(r.pattern) = ''
order by r.active desc, r.priority, r.id;

select
    'BEFORE_SUMMARY' as phase,
    sum(case when coalesce(r.active, 1) = 1 then 1 else 0 end) as active_blank_pattern_count,
    sum(case when coalesce(r.active, 1) = 0 then 1 else 0 end) as inactive_blank_pattern_count,
    sum(case
        when coalesce(r.active, 1) = 0
         and coalesce(r.remark, '') not like '%[auto-disabled: blank pattern]%'
         and coalesce(r.remark, '') not like '%[inactive legacy: blank pattern]%'
        then 1 else 0
    end) as inactive_blank_missing_remark_count
from cls_rule r
where r.pattern is null or trim(r.pattern) = '';

-- =============================================================================
-- Step 1: No-category rules (audit §5) — review only; fix via Step 3 if needed
-- =============================================================================
select
    r.id,
    r.pattern,
    r.category_id,
    r.active,
    r.remark
from cls_rule r
where r.category_id is null or trim(r.category_id) = ''
order by r.active desc, r.priority;

-- =============================================================================
-- Step 2: Fix candidates — blank pattern but category still valid (rare)
-- Review output; restore keyword in Step 3 if business value exists.
-- =============================================================================
select
    r.id,
    r.pattern,
    r.category_id,
    c.code as active_category_code,
    c.name as active_category_name,
    r.active,
    r.remark
from cls_rule r
inner join cls_category c
    on c.code = r.category_id
   and coalesce(c.deleted, 0) = 0
where (r.pattern is null or trim(r.pattern) = '')
order by r.active desc, r.priority;

-- =============================================================================
-- Step 3: MANUAL FIX — edit and uncomment before running
-- Rules:
--   - restore pattern OR assign valid active cls_category.code
--   - never change cls_category.code
-- =============================================================================

-- Example: restore keyword on a recoverable rule
-- update cls_rule
-- set pattern = '微信支付',
--     active = 1,
--     remark = trim(concat(coalesce(remark, ''), ' [restored: blank pattern remediated]'))
-- where id = '<RULE_ID>';

-- Example: assign category when pattern exists elsewhere (not blank-pattern rows)
-- update cls_rule
-- set category_id = 'DAILY-01',
--     remark = trim(concat(coalesce(remark, ''), ' [remapped category]'))
-- where id = '<RULE_ID>';

-- =============================================================================
-- Processing decision log (fill in after review; keep in ticket / runbook)
-- =============================================================================
-- | rule_id | decision              | reason                                      |
-- |---------|-----------------------|---------------------------------------------|
-- | (paste) | archive               | no keyword; never matched imports           |
-- | (paste) | restore pattern       | typo cleanup; keyword recovered from remark |
-- | (paste) | skip                  | already inactive with archive remark        |

-- =============================================================================
-- Step 4: Archive remaining ACTIVE blank-pattern rules
-- =============================================================================
update cls_rule
set active = 0,
    remark = trim(concat(
        coalesce(nullif(trim(remark), ''), ''),
        case when coalesce(nullif(trim(remark), ''), '') = '' then '' else ' ' end,
        '[inactive legacy: blank pattern]'
    ))
where id in (
    select rid from (
        select r2.id as rid
        from cls_rule r2
        where (r2.pattern is null or trim(r2.pattern) = '')
          and coalesce(r2.active, 1) = 1
          and coalesce(r2.remark, '') not like '%[inactive legacy: blank pattern]%'
    ) archive_rule_ids
);

-- =============================================================================
-- Step 5: Backfill remark on inactive blank rows still missing archive marker
-- (Includes rows only touched by V15 with [auto-disabled: blank pattern].)
-- =============================================================================
update cls_rule
set remark = trim(concat(
        coalesce(nullif(trim(remark), ''), ''),
        case when coalesce(nullif(trim(remark), ''), '') = '' then '' else ' ' end,
        '[inactive legacy: blank pattern]'
    ))
where id in (
    select rid from (
        select r2.id as rid
        from cls_rule r2
        where (r2.pattern is null or trim(r2.pattern) = '')
          and coalesce(r2.active, 1) = 0
          and coalesce(r2.remark, '') not like '%[auto-disabled: blank pattern]%'
          and coalesce(r2.remark, '') not like '%[inactive legacy: blank pattern]%'
    ) backfill_rule_ids
);

-- =============================================================================
-- Step 6: AFTER — verification
-- =============================================================================
select
    count(*) as active_blank_pattern_count
from cls_rule r
where (r.pattern is null or trim(r.pattern) = '')
  and coalesce(r.active, 1) = 1;

select
    count(*) as inactive_blank_missing_remark_count
from cls_rule r
where (r.pattern is null or trim(r.pattern) = '')
  and coalesce(r.active, 1) = 0
  and coalesce(r.remark, '') not like '%[auto-disabled: blank pattern]%'
  and coalesce(r.remark, '') not like '%[inactive legacy: blank pattern]%';

select
    'AFTER' as phase,
    r.id,
    r.pattern,
    r.category_id,
    r.active,
    r.remark
from cls_rule r
where r.pattern is null or trim(r.pattern) = ''
order by r.active desc, r.priority, r.id;
