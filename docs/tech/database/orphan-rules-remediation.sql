-- FinSight orphaned rule remediation (manual execution only).
-- Does NOT modify cls_category.code — only cls_rule.category_id / active / remark.
--
--   mysql -u <user> -p finsight < docs/tech/database/orphan-rules-remediation.sql
--
-- MySQL Workbench "safe update mode" (Error 1175): UPDATEs below use cls_rule.id (PK).
-- Alternatively: Edit -> Preferences -> SQL Editor -> uncheck "Safe Updates", reconnect.
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
-- Uses cls_rule.id in WHERE for MySQL Workbench safe-update mode (Error 1175).
-- =============================================================================
update cls_rule
set category_id = (
    select c.code
    from cls_category c
    where c.id = cls_rule.category_id
      and coalesce(c.deleted, 0) = 0
    limit 1
)
where id in (
    select rid from (
        select r2.id as rid
        from cls_rule r2
        inner join cls_category c2
            on c2.id = r2.category_id
           and coalesce(c2.deleted, 0) = 0
        where r2.category_id = c2.id
          and c2.code is not null
          and trim(c2.code) <> ''
    ) normalize_rule_ids
);

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
-- where id in (
--     select rid from (
--         select id as rid
--         from cls_rule
--         where category_id in ('OLD-CODE', 'legacy-id-here')
--           and coalesce(active, 1) = 1
--     ) remap_rule_ids
-- );

-- =============================================================================
-- Step 4: Archive remaining ACTIVE orphaned rules (inactive legacy)
-- Run only after Step 3 remaps are done.
-- =============================================================================
update cls_rule
set active = 0,
    remark = trim(concat(
        coalesce(nullif(trim(remark), ''), ''),
        case when coalesce(nullif(trim(remark), ''), '') = '' then '' else ' ' end,
        '[inactive legacy: orphan category]'
    ))
where id in (
    select rid from (
        select r2.id as rid
        from cls_rule r2
        left join cls_category c
            on c.code = r2.category_id or c.id = r2.category_id
        where coalesce(r2.category_id, '') <> ''
          and (c.id is null or coalesce(c.deleted, 0) = 1)
          and coalesce(r2.active, 1) = 1
          and coalesce(r2.remark, '') not like '%[inactive legacy: orphan category]%'
          and coalesce(r2.remark, '') not like '%[auto-disabled: orphan category]%'
    ) archive_rule_ids
);

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
