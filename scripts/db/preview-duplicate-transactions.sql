-- 查看：重复导入的交易（跨 statement，不是同账单内多次地铁/分期）
-- 指纹 = 日期 + 金额 + transaction_desc + bank_card_id + income/expense
-- 仅当同一指纹出现在 2+ 个不同 statement_id 时才算重复
-- 同一账单里多笔相同描述+金额（如地铁 3 元）不会被标为重复

-- ── 1) 汇总 ─────────────────────────────────────────────────────────────
WITH base AS (
  SELECT
    t.id,
    t.transaction_date,
    t.transaction_desc,
    t.bank_card_name,
    t.bank_card_id,
    t.statement_id,
    t.consume_name,
    ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2) AS amt,
    CASE
      WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
      WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
      WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
    END AS txn_type
  FROM transaction t
  WHERE (t.deleted IS NULL OR t.deleted = 0)
    AND (t.txn_kind IS NULL OR t.txn_kind = '' OR t.txn_kind != 'transfer')
),
ranked AS (
  SELECT
    b.*,
    ROW_NUMBER() OVER (
      PARTITION BY
        DATE(b.transaction_date),
        b.amt,
        COALESCE(b.transaction_desc, ''),
        COALESCE(b.bank_card_id, ''),
        b.txn_type
      ORDER BY
        (CASE WHEN COALESCE(b.consume_name, '') <> '' THEN 1 ELSE 0 END) DESC,
        LENGTH(COALESCE(b.transaction_desc, '')) DESC,
        COALESCE(b.statement_id, ''),
        b.id ASC
    ) AS rn,
    COUNT(*) OVER (
      PARTITION BY
        DATE(b.transaction_date),
        b.amt,
        COALESCE(b.transaction_desc, ''),
        COALESCE(b.bank_card_id, ''),
        b.txn_type
    ) AS group_size,
    COUNT(DISTINCT COALESCE(b.statement_id, '')) OVER (
      PARTITION BY
        DATE(b.transaction_date),
        b.amt,
        COALESCE(b.transaction_desc, ''),
        COALESCE(b.bank_card_id, ''),
        b.txn_type
    ) AS statement_variants
  FROM base b
  WHERE b.txn_type IN ('income', 'expense')
)
SELECT
  COALESCE(SUM(CASE WHEN r.rn > 1 THEN 1 ELSE 0 END), 0) AS rows_to_delete,
  COALESCE(SUM(CASE WHEN r.rn = 1 THEN 1 ELSE 0 END), 0) AS rows_to_keep
FROM ranked r
WHERE r.group_size > 1
  AND r.statement_variants > 1;

-- ── 2) 明细（最多 500 行）────────────────────────────────────────────────
WITH base AS (
  SELECT
    t.id,
    t.transaction_date,
    t.transaction_desc,
    t.bank_card_name,
    t.bank_card_id,
    t.statement_id,
    t.consume_name,
    ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2) AS amt,
    CASE
      WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
      WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
      WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
    END AS txn_type
  FROM transaction t
  WHERE (t.deleted IS NULL OR t.deleted = 0)
    AND (t.txn_kind IS NULL OR t.txn_kind = '' OR t.txn_kind != 'transfer')
),
ranked AS (
  SELECT
    b.*,
    ROW_NUMBER() OVER (
      PARTITION BY
        DATE(b.transaction_date),
        b.amt,
        COALESCE(b.transaction_desc, ''),
        COALESCE(b.bank_card_id, ''),
        b.txn_type
      ORDER BY
        (CASE WHEN COALESCE(b.consume_name, '') <> '' THEN 1 ELSE 0 END) DESC,
        LENGTH(COALESCE(b.transaction_desc, '')) DESC,
        COALESCE(b.statement_id, ''),
        b.id ASC
    ) AS rn,
    COUNT(*) OVER (
      PARTITION BY
        DATE(b.transaction_date),
        b.amt,
        COALESCE(b.transaction_desc, ''),
        COALESCE(b.bank_card_id, ''),
        b.txn_type
    ) AS group_size,
    COUNT(DISTINCT COALESCE(b.statement_id, '')) OVER (
      PARTITION BY
        DATE(b.transaction_date),
        b.amt,
        COALESCE(b.transaction_desc, ''),
        COALESCE(b.bank_card_id, ''),
        b.txn_type
    ) AS statement_variants
  FROM base b
  WHERE b.txn_type IN ('income', 'expense')
)
SELECT
  CASE WHEN r.rn = 1 THEN 'KEEP' ELSE 'DELETE' END AS action,
  r.group_size,
  r.statement_variants,
  r.id,
  r.transaction_date,
  r.txn_type,
  r.amt,
  r.transaction_desc,
  r.bank_card_name,
  r.statement_id
FROM ranked r
WHERE r.group_size > 1
  AND r.statement_variants > 1
ORDER BY r.transaction_date DESC, r.amt DESC, r.id
LIMIT 500;
