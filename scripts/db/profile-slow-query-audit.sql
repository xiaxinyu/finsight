-- Profile / concentration slow-query audit helpers (v2.0.0).
-- Run against production-like data after enabling slow_query_log.

-- 1) Enable slow query log (session or global — adjust for your MySQL policy):
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 0.5;

-- 2) Explain profile concentration query pattern (replace dates and user):
EXPLAIN
SELECT v.category_code, v.category_name, SUM(v.amount) AS amount
FROM v_transaction_analytics v
INNER JOIN transaction t ON t.id = v.id
WHERE v.direction = 'expense' AND v.is_transfer = 0 AND v.is_refund = 0
  AND v.amount > 0
  AND v.category_code IS NOT NULL AND v.category_code != '' AND v.category_code != '__UNCLASSIFIED__'
  AND v.txn_date >= '2025-07-01' AND v.txn_date < '2026-07-01'
  AND (t.created_by = 'your_user' OR ('your_user' = '_anonymous' AND t.created_by IS NULL))
GROUP BY v.category_code, v.category_name
ORDER BY amount DESC;

-- 3) Verify V30 indexes exist:
SELECT index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'transaction'
  AND index_name IN (
    'idx_txn_owner_deleted_date',
    'idx_txn_consume_code',
    'idx_txn_kind',
    'idx_txn_bank_card'
  )
ORDER BY index_name, seq_in_index;
