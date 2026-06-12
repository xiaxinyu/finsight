-- Soft-delete duplicate transactions imported today (keep the oldest row per fingerprint).
-- Fingerprint: date + amount + description + bank_card_id (matches app dedup logic).
--
-- 1) PREVIEW — run first and verify row count
-- 2) UPDATE — uncomment after preview looks correct
-- 3) Adjust @import_day if not running on the import day

SET @import_day = CURDATE();

WITH ranked AS (
  SELECT
    t.id,
    t.transaction_date,
    t.transaction_desc,
    ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)) AS amt,
    t.bank_card_id,
    t.statement_id,
    ROW_NUMBER() OVER (
      PARTITION BY
        DATE_FORMAT(t.transaction_date, '%Y-%m-%d'),
        ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2),
        COALESCE(t.transaction_desc, ''),
        COALESCE(t.bank_card_id, '')
      ORDER BY t.id ASC
    ) AS rn
  FROM transaction t
  WHERE (t.deleted IS NULL OR t.deleted = 0)
    AND (t.txn_kind IS NULL OR t.txn_kind = '' OR t.txn_kind != 'transfer')
    AND (
      t.statement_id IN (
        SELECT st.id FROM statement st
        WHERE DATE(COALESCE(st.updated_at, st.created_at)) = @import_day
          AND (st.deleted IS NULL OR st.deleted = 0)
      )
      OR DATE(COALESCE(t.created_at, t.createtime)) = @import_day
    )
)
SELECT id, transaction_date, transaction_desc, amt, bank_card_id, statement_id
FROM ranked
WHERE rn > 1
ORDER BY transaction_date DESC, amt DESC;

-- UPDATE transaction t
-- INNER JOIN (
--   SELECT id FROM (
--     SELECT
--       t.id,
--       ROW_NUMBER() OVER (
--         PARTITION BY
--           DATE_FORMAT(t.transaction_date, '%Y-%m-%d'),
--           ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2),
--           COALESCE(t.transaction_desc, ''),
--           COALESCE(t.bank_card_id, '')
--         ORDER BY t.id ASC
--       ) AS rn
--     FROM transaction t
--     WHERE (t.deleted IS NULL OR t.deleted = 0)
--       AND (t.txn_kind IS NULL OR t.txn_kind = '' OR t.txn_kind != 'transfer')
--       AND (
--         t.statement_id IN (
--           SELECT st.id FROM statement st
--           WHERE DATE(COALESCE(st.updated_at, st.created_at)) = @import_day
--             AND (st.deleted IS NULL OR st.deleted = 0)
--         )
--         OR DATE(COALESCE(t.created_at, t.createtime)) = @import_day
--       )
--   ) x WHERE x.rn > 1
-- ) d ON d.id = t.id
-- SET t.deleted = 1,
--     t.updated_at = NOW();
