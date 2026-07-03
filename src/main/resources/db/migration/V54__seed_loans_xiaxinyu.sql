-- Seed loan portfolio from user's loan summary spreadsheet (xiaxinyu).
-- Idempotent: skips rows that already exist by fixed id.

INSERT INTO fin_loan (
    id, user_id, name, lender_name, lender_bank_code,
    principal_amount, outstanding_balance, interest_rate_pct, monthly_payment,
    repayment_method, maturity_date, disbursement_card_id, repayment_card_id,
    status, notes, sort_order, deleted,
    created_by, created_at, updated_by, updated_at
)
SELECT * FROM (
    SELECT 'loan-xiaxinyu-bocom-126k' AS id, 'xiaxinyu' AS user_id, NULL AS name,
           '交通银行' AS lender_name, 'BOCOM' AS lender_bank_code,
           126000.00 AS principal_amount, 126000.00 AS outstanding_balance, 5.8600 AS interest_rate_pct, 635.00 AS monthly_payment,
           NULL AS repayment_method, '2028-03-01' AS maturity_date,
           'id-ccb-d-001' AS disbursement_card_id, NULL AS repayment_card_id,
           'ACTIVE' AS status, '2028年3月到期 · 放款卡待核对（系统暂无交通银行户）' AS notes, 0 AS sort_order, 0 AS deleted,
           'xiaxinyu' AS created_by, NOW(3) AS created_at, 'xiaxinyu' AS updated_by, NOW(3) AS updated_at
    UNION ALL SELECT 'loan-xiaxinyu-abc-205k', 'xiaxinyu', NULL,
           '农业银行', 'ABC',
           205000.00, 205000.00, 5.6400, 5749.00,
           'EQUAL_INSTALLMENT', NULL,
           'id-ccb-d-001', NULL,
           'ACTIVE', '等额本息 · 放款卡待核对（系统暂无农业银行户）', 1, 0,
           'xiaxinyu', NOW(3), 'xiaxinyu', NOW(3)
    UNION ALL SELECT 'loan-xiaxinyu-czb-5880k', 'xiaxinyu', NULL,
           '浙商银行', 'CZB',
           5880000.00, 5880000.00, 5.2500, 25725.00,
           NULL, '2028-07-18',
           'id-ccb-d-001', NULL,
           'ACTIVE', '2028年7月18日到期 · 放款卡待核对（系统暂无浙商银行户）', 2, 0,
           'xiaxinyu', NOW(3), 'xiaxinyu', NOW(3)
    UNION ALL SELECT 'loan-xiaxinyu-dgb-490k', 'xiaxinyu', NULL,
           '东莞银行', 'DGB',
           490000.00, 490000.00, 4.8000, 7020.00,
           'EQUAL_PRINCIPAL', NULL,
           'id-crbank-d-001', NULL,
           'ACTIVE', '等额本金 · 放款卡待核对（系统暂无东莞银行户）', 3, 0,
           'xiaxinyu', NOW(3), 'xiaxinyu', NOW(3)
    UNION ALL SELECT 'loan-xiaxinyu-ccb-140k', 'xiaxinyu', NULL,
           '建设银行', 'CCB',
           140000.00, 140000.00, 4.2900, 500.00,
           NULL, NULL,
           'id-ccb-d-001', NULL,
           'ACTIVE', NULL, 4, 0,
           'xiaxinyu', NOW(3), 'xiaxinyu', NOW(3)
    UNION ALL SELECT 'loan-xiaxinyu-cmb-200k', 'xiaxinyu', NULL,
           '招商银行', 'CMB',
           200000.00, 200000.00, 3.0000, 516.00,
           NULL, NULL,
           'id-cmb-d-001', NULL,
           'ACTIVE', NULL, 5, 0,
           'xiaxinyu', NOW(3), 'xiaxinyu', NOW(3)
    UNION ALL SELECT 'loan-xiaxinyu-ccb-650k', 'xiaxinyu', NULL,
           '建设银行', 'CCB',
           650000.00, 650000.00, 2.8000, 1496.00,
           'INTEREST_FIRST', '2028-03-28',
           'id-ccb-d-002', NULL,
           'ACTIVE', '先息后本 · 2028年3月28日到期', 6, 0,
           'xiaxinyu', NOW(3), 'xiaxinyu', NOW(3)
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM fin_loan l WHERE l.id = seed.id
);
