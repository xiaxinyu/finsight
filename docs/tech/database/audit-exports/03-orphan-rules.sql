-- §3 Orphaned rules — export baseline-orphan-rules.csv
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
