-- Manual L1 dedup fallback (Issue #69 / category merge playbook).
-- Prefer Categories UI: merge INCOME -> INC (L1 into L1).
-- Does NOT batch-update transactions on L1 merge (children keep their codes).

-- Preview duplicate L1 by name
select name, group_concat(code order by code) as codes, count(*) as cnt
from cls_category
where coalesce(deleted, 0) = 0
  and coalesce(level, 1) = 1
group by name
having count(*) > 1;

-- Example: reparent INCOME children to INC (only if UI merge unavailable)
-- update cls_category set parent_id = 'INC', level = 2, updated_at = now()
-- where parent_id = 'INCOME' and coalesce(deleted, 0) = 0;

-- update cls_category set deleted = 1, updated_at = now()
-- where code = 'INCOME' and level = 1 and coalesce(deleted, 0) = 0;
