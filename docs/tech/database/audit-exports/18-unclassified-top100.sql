-- §18 Top unclassified raw descriptions — export baseline-unclassified-top100.csv
select
    coalesce(nullif(trim(opponent_name), ''), nullif(trim(transaction_desc), ''), nullif(trim(memo), ''), 'UNKNOWN') as raw_text,
    count(*) as txn_count,
    round(sum(abs(coalesce(balance_money, 0) + coalesce(income_money, 0))), 2) as amount
from `transaction`
where (deleted is null or deleted = 0)
  and coalesce(trim(consume_code), '') = ''
  and coalesce(trim(consume_name), '') = ''
group by raw_text
order by txn_count desc, amount desc
limit 100;
