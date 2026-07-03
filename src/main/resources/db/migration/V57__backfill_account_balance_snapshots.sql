-- Backfill latest-per-day balances from existing transactions (one row per user/card/date).
INSERT INTO fin_account_balance_snapshot (
    id, user_id, card_id, balance, snapshot_date, source, created_by, created_at
)
SELECT
    UUID(),
    ranked.created_by,
    ranked.bank_card_id,
    ROUND(ranked.account_balance, 2),
    DATE(ranked.transaction_date),
    'import_backfill',
    'system',
    NOW(3)
FROM (
    SELECT
        t.created_by,
        t.bank_card_id,
        t.account_balance,
        t.transaction_date,
        ROW_NUMBER() OVER (
            PARTITION BY t.created_by, t.bank_card_id, DATE(t.transaction_date)
            ORDER BY t.transaction_date DESC, t.id DESC
        ) AS rn
    FROM transaction t
    WHERE t.account_balance IS NOT NULL
      AND t.bank_card_id IS NOT NULL
      AND LENGTH(TRIM(t.bank_card_id)) > 0
      AND COALESCE(t.deleted, 0) != 1
      AND t.created_by IS NOT NULL
      AND LENGTH(TRIM(t.created_by)) > 0
) ranked
WHERE ranked.rn = 1
ON DUPLICATE KEY UPDATE
    balance = VALUES(balance),
    source = VALUES(source),
    created_at = NOW(3);
