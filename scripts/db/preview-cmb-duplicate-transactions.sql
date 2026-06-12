-- 查看：招商银行重复交易（同一天 + 同金额 + 同类型，不要求描述完全一致）
-- 先看最上面的汇总；为 0 表示没有可删的重复（可能之前已经删过）

SET @bank_code = 'CMB';

SELECT
  SUM(CASE WHEN r.rn > 1 THEN 1 ELSE 0 END) AS rows_to_delete,
  SUM(CASE WHEN r.rn = 1 THEN 1 ELSE 0 END) AS rows_to_keep
FROM (
  SELECT
    CASE
      WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
      WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
      WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
    END AS resolved_type,
    ROW_NUMBER() OVER (
      PARTITION BY
        DATE(t.transaction_date),
        ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2),
        CASE
          WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
          WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
          WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
        END
      ORDER BY
        (CASE WHEN COALESCE(t.consume_name, '') <> '' THEN 1 ELSE 0 END) DESC,
        LENGTH(COALESCE(t.transaction_desc, '')) DESC,
        t.id ASC
    ) AS rn,
    COUNT(*) OVER (
      PARTITION BY
        DATE(t.transaction_date),
        ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2),
        CASE
          WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
          WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
          WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
        END
    ) AS group_size
  FROM transaction t
  LEFT JOIN fin_bank_account bc
    ON bc.id = t.bank_card_id AND (bc.deleted IS NULL OR bc.deleted != 1)
  LEFT JOIN statement st
    ON st.id = t.statement_id AND (st.deleted IS NULL OR st.deleted != 1)
  WHERE (t.deleted IS NULL OR t.deleted = 0)
    AND (t.txn_kind IS NULL OR t.txn_kind = '' OR t.txn_kind != 'transfer')
    AND (
      UPPER(TRIM(COALESCE(bc.bank_code, ''))) = @bank_code
      OR UPPER(TRIM(COALESCE(st.source_bank_code, ''))) = @bank_code
      OR COALESCE(t.bank_card_name, bc.card_name, '') LIKE '%招商%'
    )
) r
WHERE r.group_size > 1
  AND r.resolved_type IN ('income', 'expense');

-- 明细（rows_to_delete = 0 时下面无结果）
SELECT
  CASE WHEN r.rn = 1 THEN 'KEEP' ELSE 'DELETE' END AS action,
  r.group_size,
  r.id,
  r.transaction_date,
  r.resolved_type AS txn_type,
  r.amt,
  r.transaction_desc,
  r.consume_name,
  r.bank_card_name,
  r.statement_id
FROM (
  SELECT
    t.id,
    t.transaction_date,
    t.transaction_desc,
    t.consume_name,
    t.bank_card_name,
    t.statement_id,
    CASE
      WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
      WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
      WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
    END AS resolved_type,
    ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2) AS amt,
    ROW_NUMBER() OVER (
      PARTITION BY
        DATE(t.transaction_date),
        ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2),
        CASE
          WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
          WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
          WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
        END
      ORDER BY
        (CASE WHEN COALESCE(t.consume_name, '') <> '' THEN 1 ELSE 0 END) DESC,
        LENGTH(COALESCE(t.transaction_desc, '')) DESC,
        t.id ASC
    ) AS rn,
    COUNT(*) OVER (
      PARTITION BY
        DATE(t.transaction_date),
        ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2),
        CASE
          WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
          WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
          WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
        END
    ) AS group_size
  FROM transaction t
  LEFT JOIN fin_bank_account bc
    ON bc.id = t.bank_card_id AND (bc.deleted IS NULL OR bc.deleted != 1)
  LEFT JOIN statement st
    ON st.id = t.statement_id AND (st.deleted IS NULL OR st.deleted != 1)
  WHERE (t.deleted IS NULL OR t.deleted = 0)
    AND (t.txn_kind IS NULL OR t.txn_kind = '' OR t.txn_kind != 'transfer')
    AND (
      UPPER(TRIM(COALESCE(bc.bank_code, ''))) = @bank_code
      OR UPPER(TRIM(COALESCE(st.source_bank_code, ''))) = @bank_code
      OR COALESCE(t.bank_card_name, bc.card_name, '') LIKE '%招商%'
    )
) r
WHERE r.group_size > 1
  AND r.resolved_type IN ('income', 'expense')
ORDER BY r.transaction_date DESC, r.amt DESC, r.id;
