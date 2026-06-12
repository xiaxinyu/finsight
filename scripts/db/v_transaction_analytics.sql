-- Manual (re)create analytics view — same definition as Flyway V18.
-- mysql ... finsight < scripts/db/v_transaction_analytics.sql
CREATE OR REPLACE VIEW v_transaction_analytics AS
SELECT
    t.id,
    t.transaction_date AS txn_date,
    CASE
        WHEN COALESCE(t.txn_kind, '') IN ('income', 'expense') THEN t.txn_kind
        WHEN COALESCE(t.income_money, 0) > 0 OR COALESCE(t.balance_money, 0) < 0 THEN 'income'
        WHEN COALESCE(t.balance_money, 0) > 0 THEN 'expense'
        ELSE ''
    END AS direction,
    ROUND(ABS(COALESCE(t.balance_money, 0) + COALESCE(t.income_money, 0)), 2) AS amount,
    COALESCE(NULLIF(TRIM(t.consume_code), ''), '__UNCLASSIFIED__') AS category_code,
    COALESCE(NULLIF(TRIM(cat.name), ''), NULLIF(TRIM(t.consume_name), ''), '未分类') AS category_name,
    COALESCE(NULLIF(TRIM(parent.code), ''), NULLIF(TRIM(cat.code), ''), '__UNCLASSIFIED__') AS category_l1_code,
    COALESCE(NULLIF(TRIM(parent.name), ''), NULLIF(TRIM(cat.name), ''), '未分类') AS category_l1_name,
    COALESCE(NULLIF(TRIM(bc.bank_code), ''), NULLIF(TRIM(st.source_bank_code), '')) AS bank_code,
    COALESCE(NULLIF(TRIM(bc.card_type_code), ''), NULLIF(TRIM(t.card_type_name), '')) AS card_type_code,
    t.transaction_desc,
    t.opponent_name,
    t.memo,
    CASE WHEN t.txn_kind = 'transfer' THEN 1 ELSE 0 END AS is_transfer,
    t.statement_id
FROM transaction t
LEFT JOIN fin_bank_account bc ON bc.id = t.bank_card_id AND (bc.deleted IS NULL OR bc.deleted != 1)
LEFT JOIN statement st ON st.id = t.statement_id AND (st.deleted IS NULL OR st.deleted != 1)
LEFT JOIN cls_category cat ON cat.code = t.consume_code AND cat.deleted != 1
LEFT JOIN cls_category parent ON parent.id = cat.parent_id AND parent.deleted != 1
WHERE (t.deleted IS NULL OR t.deleted = 0);
