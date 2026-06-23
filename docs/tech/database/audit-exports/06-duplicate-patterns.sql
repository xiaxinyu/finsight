-- §6 Duplicate active rule patterns — export baseline-duplicate-patterns.csv
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
