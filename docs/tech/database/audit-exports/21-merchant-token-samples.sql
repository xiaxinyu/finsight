-- §21 Merchant token normalization samples — export baseline-merchant-token-samples.csv
select
    t.id,
    t.transaction_date,
    coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), '') as raw_merchant,
    finsight_normalize_merchant_token(
        coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), '')) as normalized_token,
    v.merchant_token as analytics_view_token,
    t.consume_code,
    round(abs(coalesce(t.expense_amount, 0)), 2) as amount
from `transaction` t
left join v_transaction_analytics v on v.id = t.id
where coalesce(t.deleted, 0) = 0
  and abs(coalesce(t.expense_amount, 0)) > 0
  and (
      coalesce(trim(v.merchant_token), '') = ''
      or finsight_normalize_merchant_token(
          coalesce(nullif(trim(t.opponent_name), ''), nullif(trim(t.transaction_desc), ''), ''))
         <> coalesce(v.merchant_token, '')
  )
order by t.transaction_date desc
limit 100;
