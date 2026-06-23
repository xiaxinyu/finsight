-- §11 Transaction category field drift — export baseline-field-drift.csv
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
