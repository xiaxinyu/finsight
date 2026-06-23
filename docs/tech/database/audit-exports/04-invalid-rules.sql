-- §4 Invalid rules with blank pattern — export baseline-invalid-rules.csv
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
