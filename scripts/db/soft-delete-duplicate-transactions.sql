-- 软删除：重复导入的交易（与 preview-duplicate-transactions.sql 同一规则）
-- 只删跨 statement 的重复；同账单内多次地铁/分期不会动
-- 1) 先跑 preview，确认 rows_to_delete
-- 2) 执行本文件，看 soft_deleted_rows

UPDATE transaction t
INNER JOIN (
  SELECT x.id
  FROM (
    SELECT
      t.id,
      CASE
        WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
        WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
        WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
      END AS txn_type,
      COUNT(DISTINCT COALESCE(t.statement_id, '')) OVER (
        PARTITION BY
          DATE(t.transaction_date),
          ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2),
          COALESCE(t.transaction_desc, ''),
          COALESCE(t.bank_card_id, ''),
          CASE
            WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
            WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
            WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
          END
      ) AS statement_variants,
      ROW_NUMBER() OVER (
        PARTITION BY
          DATE(t.transaction_date),
          ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2),
          COALESCE(t.transaction_desc, ''),
          COALESCE(t.bank_card_id, ''),
          CASE
            WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
            WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
            WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
          END
        ORDER BY
          (CASE WHEN COALESCE(t.consume_name, '') <> '' THEN 1 ELSE 0 END) DESC,
          LENGTH(COALESCE(t.transaction_desc, '')) DESC,
          COALESCE(t.statement_id, ''),
          t.id ASC
      ) AS rn
    FROM transaction t
    WHERE (t.deleted IS NULL OR t.deleted = 0)
      AND (t.txn_kind IS NULL OR t.txn_kind = '' OR t.txn_kind != 'transfer')
  ) x
  WHERE x.rn > 1
    AND x.txn_type IN ('income', 'expense')
    AND x.statement_variants > 1
) dup ON dup.id = t.id
SET t.deleted = 1;

SELECT ROW_COUNT() AS soft_deleted_rows;
