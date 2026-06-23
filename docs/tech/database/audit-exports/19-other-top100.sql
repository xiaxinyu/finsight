-- §19 Top OTHER / catch-all category transactions — export baseline-other-consumption-top100.csv
select
    t.id,
    t.transaction_date,
    t.consume_code,
    t.consume_name,
    coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), 'UNKNOWN') as raw_text,
    round(abs(coalesce(t.expense_amount, 0)) + abs(coalesce(t.income_money, 0)), 2) as amount
from `transaction` t
left join cls_category c on c.code = t.consume_code
where (t.deleted is null or t.deleted = 0)
  and coalesce(trim(t.consume_code), '') <> ''
  and (
      t.consume_code like 'OTHER%'
      or coalesce(c.name, t.consume_name, '') like '%其它%'
      or coalesce(c.name, t.consume_name, '') like '%无法归类%'
  )
order by amount desc, t.transaction_date desc
limit 100;
