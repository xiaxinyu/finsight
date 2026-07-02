-- Loan registry: lender bank → disbursement card, optional repayment card, transaction links.

CREATE TABLE IF NOT EXISTS fin_loan (
    id                      VARCHAR(64)   NOT NULL PRIMARY KEY,
    user_id                 VARCHAR(64)   NOT NULL,
    name                    VARCHAR(128)  NULL COMMENT 'Display label',
    lender_name             VARCHAR(128)  NOT NULL COMMENT 'Lending institution',
    lender_bank_code        VARCHAR(32)   NULL COMMENT 'Optional code aligned with fin_bank_account.bank_code',
    principal_amount        DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT 'Original / total facility',
    outstanding_balance     DECIMAL(18,2) NULL COMMENT 'Current remaining; defaults to principal on create',
    interest_rate_pct       DECIMAL(8,4)  NULL COMMENT 'Annual rate e.g. 5.8600',
    monthly_payment         DECIMAL(18,2) NULL,
    repayment_method        VARCHAR(32)   NULL COMMENT 'EQUAL_INSTALLMENT|EQUAL_PRINCIPAL|INTEREST_FIRST|BULLET|OTHER',
    maturity_date           DATE          NULL,
    disbursement_card_id    VARCHAR(64)   NULL COMMENT 'Card that receives loan proceeds',
    repayment_card_id       VARCHAR(64)   NULL COMMENT 'Card debited for repayments (optional)',
    status                  VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|CLOSED',
    notes                   VARCHAR(512)  NULL,
    sort_order              INT           NOT NULL DEFAULT 0,
    deleted                 TINYINT(1)    NOT NULL DEFAULT 0,
    created_by              VARCHAR(64)   NULL,
    created_at              DATETIME(3)   NULL,
    updated_by              VARCHAR(64)   NULL,
    updated_at              DATETIME(3)   NULL,
    KEY idx_fin_loan_user (user_id, deleted, status),
    KEY idx_fin_loan_disburse_card (disbursement_card_id),
    KEY idx_fin_loan_rate (user_id, interest_rate_pct)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_loan_txn_link (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    loan_id         VARCHAR(64)  NOT NULL,
    transaction_id  VARCHAR(64)  NOT NULL,
    link_type       VARCHAR(16)  NOT NULL DEFAULT 'REPAYMENT' COMMENT 'DISBURSEMENT|REPAYMENT|INTEREST|OTHER',
    user_id         VARCHAR(64)  NOT NULL,
    created_by      VARCHAR(64)  NULL,
    created_at      DATETIME(3)  NULL,
    UNIQUE KEY uk_fin_loan_txn (loan_id, transaction_id),
    KEY idx_fin_loan_txn_loan (loan_id),
    KEY idx_fin_loan_txn_txn (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
