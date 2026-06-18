-- FinSight orphaned rule remediation (manual execution only).
-- Does NOT modify cls_category.code — only cls_rule.category_id / active / remark.
--
--   mysql -u <user> -p finsight < docs/tech/database/orphan-rules-remediation.sql
--
-- Workflow:
--   1. Run BEFORE inventory (Step 0) and save results.
--   2. Review Step 2 remap candidates; edit Step 3 with your mappings.
--   3. Run Step 3 remaps, then Step 4 archive remaining active orphans.
--   4. Run AFTER verification (Step 5) — active orphaned count should be 0.

-- =============================================================================
-- Step 0: BEFORE — orphaned rule inventory (audit #3)
-- =============================================================================
select
    'BEFORE' as phase,
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

-- =============================================================================
-- Step 1: Normalize cls_rule.category_id from legacy category id → active code
-- (Safe: does not change category codes, only rule pointers.)
-- =============================================================================
update cls_rule r
inner join cls_category c
    on c.id = r.category_id
   and coalesce(c.deleted, 0) = 0
set r.category_id = c.code
where r.category_id = c.id
  and c.code is not null
  and trim(c.code) <> '';

-- =============================================================================
-- Step 2: Remap candidates — deleted categories still referenced by rules
-- Review output, then add explicit UPDATE statements in Step 3.
-- =============================================================================
select
    r.category_id as orphan_category_ref,
    count(*) as rule_count,
    group_concat(r.id order by r.priority separator ', ') as rule_ids,
    max(c.name) as deleted_category_name,
    max(c.code) as deleted_category_code
from cls_rule r
left join cls_category active_cat
    on active_cat.code = r.category_id
   and coalesce(active_cat.deleted, 0) = 0
left join cls_category c
    on c.code = r.category_id or c.id = r.category_id
where coalesce(r.category_id, '') <> ''
  and active_cat.id is null
  and coalesce(r.active, 1) = 1
group by r.category_id
order by rule_count desc;

-- Optional: name-based suggestions (review only — do not auto-apply)
select
    orphan.category_id as orphan_ref,
    orphan.rule_count,
    orphan.deleted_name,
    suggest.code as suggested_active_code,
    suggest.name as suggested_active_name
from (
    select
        r.category_id,
        count(*) as rule_count,
        max(c.name) as deleted_name
    from cls_rule r
    left join cls_category active_cat
        on active_cat.code = r.category_id and coalesce(active_cat.deleted, 0) = 0
    left join cls_category c on c.code = r.category_id or c.id = r.category_id
    where coalesce(r.category_id, '') <> ''
      and active_cat.id is null
      and coalesce(r.active, 1) = 1
    group by r.category_id
) orphan
left join cls_category suggest
    on coalesce(suggest.deleted, 0) = 0
   and suggest.name = orphan.deleted_name
order by orphan.rule_count desc;

-- =============================================================================
-- Step 3: MANUAL REMAP — edit and uncomment before running
-- Rules:
--   - target must be an active cls_category.code (never change category codes)
--   - verify: select code, name, deleted from cls_category where code = '<TARGET>';
-- =============================================================================

-- Example (replace values, then uncomment):
-- update cls_rule
-- set category_id = 'DAILY-01',
--     remark = trim(concat(coalesce(remark, ''), ' [remapped from OLD-CODE]'))
-- where category_id in ('OLD-CODE', 'legacy-id-here')
--   and coalesce(active, 1) = 1;

-- =============================================================================
-- Step 4: Archive remaining ACTIVE orphaned rules (inactive legacy)
-- Run only after Step 3 remaps are done.
-- =============================================================================
update cls_rule r
left join cls_category c
    on c.code = r.category_id or c.id = r.category_id
set r.active = 0,
    r.remark = trim(concat(
        coalesce(nullif(trim(r.remark), ''), ''),
        case when coalesce(nullif(trim(r.remark), ''), '') = '' then '' else ' ' end,
        '[inactive legacy: orphan category]'
    ))
where coalesce(r.category_id, '') <> ''
  and (c.id is null or coalesce(c.deleted, 0) = 1)
  and coalesce(r.active, 1) = 1
  and coalesce(r.remark, '') not like '%[inactive legacy: orphan category]%'
  and coalesce(r.remark, '') not like '%[auto-disabled: orphan category]%';

-- =============================================================================
-- Step 5: AFTER — verification (active orphans should be 0)
-- =============================================================================
select
    count(*) as active_orphan_count
from cls_rule r
left join cls_category c
    on c.code = r.category_id or c.id = r.category_id
where coalesce(r.category_id, '') <> ''
  and (c.id is null or coalesce(c.deleted, 0) = 1)
  and coalesce(r.active, 1) = 1;

select
    'AFTER' as phase,
    r.id,
    r.pattern,
    r.category_id,
    r.active,
    r.remark
from cls_rule r
left join cls_category c
    on c.code = r.category_id or c.id = r.category_id
where coalesce(r.category_id, '') <> ''
  and (c.id is null or coalesce(c.deleted, 0) = 1)
order by r.active desc, r.priority, r.pattern;

-- Archived legacy orphans (expected after Step 4 if any were unmappable)
select
    count(*) as archived_legacy_orphan_count
from cls_rule r
left join cls_category c
    on c.code = r.category_id or c.id = r.category_id
where coalesce(r.category_id, '') <> ''
  and (c.id is null or coalesce(c.deleted, 0) = 1)
  and coalesce(r.active, 1) = 0
  and (
      coalesce(r.remark, '') like '%[inactive legacy: orphan category]%'
      or coalesce(r.remark, '') like '%[auto-disabled: orphan category]%'
  );
