-- 软删除：招商银行重复交易
-- 1) 先跑 preview-cmb-duplicate-transactions.sql，确认 rows_to_delete > 0
-- 2) 执行本文件；看最后一行 soft_deleted_rows，不是 COMMIT
--    COMMIT 永远显示 0 row(s)，可忽略

SET @bank_code = 'CMB';

UPDATE transaction t
INNER JOIN (
  SELECT x.id
  FROM (
    SELECT
      t.id,
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
      CASE
        WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
        WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
        WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
      END AS resolved_type
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
  ) x
  WHERE x.rn > 1
    AND x.resolved_type IN ('income', 'expense')
) dup ON dup.id = t.id
SET t.deleted = 1;

SELECT ROW_COUNT() AS soft_deleted_rows;
