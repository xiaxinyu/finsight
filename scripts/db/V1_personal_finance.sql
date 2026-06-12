-- FinSight personal finance schema (manual, idempotent)
-- Usage: see scripts/db/apply-personal-finance-schema.sh
-- Database: finsight (must exist before running)

USE finsight;

CREATE TABLE IF NOT EXISTS financial_account (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    account_type VARCHAR(32) NULL,
    bank_card_id VARCHAR(64) NULL,
    currency VARCHAR(8) DEFAULT 'CNY',
    is_liability TINYINT DEFAULT 0,
    display_order INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    createUser VARCHAR(64) NULL,
    createTime DATETIME NULL,
    updateUser VARCHAR(64) NULL,
    updateTime DATETIME NULL,
    INDEX idx_fa_bank_card (bank_card_id),
    INDEX idx_fa_deleted (deleted)
);

CREATE TABLE IF NOT EXISTS account_balance_snapshot (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    snapshot_date DATE NOT NULL,
    balance DECIMAL(18,2) NOT NULL DEFAULT 0,
    source VARCHAR(32) NULL,
    createUser VARCHAR(64) NULL,
    createTime DATETIME NULL,
    updateUser VARCHAR(64) NULL,
    updateTime DATETIME NULL,
    INDEX idx_abs_account_date (account_id, snapshot_date)
);

CREATE TABLE IF NOT EXISTS transfer_pair (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    from_account_id VARCHAR(64) NULL,
    to_account_id VARCHAR(64) NULL,
    from_transaction_id VARCHAR(64) NULL,
    to_transaction_id VARCHAR(64) NULL,
    amount DECIMAL(18,2) NULL,
    transfer_date DATE NULL,
    transfer_group_id VARCHAR(64) NULL,
    memo VARCHAR(512) NULL,
    deleted TINYINT DEFAULT 0,
    createUser VARCHAR(64) NULL,
    createTime DATETIME NULL,
    updateUser VARCHAR(64) NULL,
    updateTime DATETIME NULL,
    INDEX idx_tp_group (transfer_group_id)
);

CREATE TABLE IF NOT EXISTS transaction_link (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    from_transaction_id VARCHAR(64) NULL,
    to_transaction_id VARCHAR(64) NULL,
    link_type VARCHAR(32) NULL,
    confidence DECIMAL(5,2) NULL,
    createUser VARCHAR(64) NULL,
    createTime DATETIME NULL,
    updateUser VARCHAR(64) NULL,
    updateTime DATETIME NULL
);

CREATE TABLE IF NOT EXISTS budget (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NULL,
    period_type VARCHAR(16) NULL,
    year INT NULL,
    month INT NULL,
    deleted TINYINT DEFAULT 0,
    createUser VARCHAR(64) NULL,
    createTime DATETIME NULL,
    updateUser VARCHAR(64) NULL,
    updateTime DATETIME NULL,
    INDEX idx_budget_period (period_type, year, month)
);

CREATE TABLE IF NOT EXISTS budget_line (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    budget_id VARCHAR(64) NOT NULL,
    category_code VARCHAR(64) NULL,
    bucket_key VARCHAR(64) NULL,
    limit_amount DECIMAL(18,2) NULL,
    rollover TINYINT DEFAULT 0,
    createUser VARCHAR(64) NULL,
    createTime DATETIME NULL,
    updateUser VARCHAR(64) NULL,
    updateTime DATETIME NULL,
    INDEX idx_bl_budget (budget_id)
);

CREATE TABLE IF NOT EXISTS bill (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    amount DECIMAL(18,2) NULL,
    due_day INT NULL,
    recurrence VARCHAR(32) NULL,
    account_id VARCHAR(64) NULL,
    category_code VARCHAR(64) NULL,
    enabled TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    createUser VARCHAR(64) NULL,
    createTime DATETIME NULL,
    updateUser VARCHAR(64) NULL,
    updateTime DATETIME NULL,
    INDEX idx_bill_due (due_day, enabled)
);

CREATE TABLE IF NOT EXISTS financial_goal (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    goal_type VARCHAR(32) NULL,
    target_amount DECIMAL(18,2) NULL,
    current_amount DECIMAL(18,2) NULL,
    target_date DATE NULL,
    monthly_contribution DECIMAL(18,2) NULL,
    linked_account_id VARCHAR(64) NULL,
    deleted TINYINT DEFAULT 0,
    createUser VARCHAR(64) NULL,
    createTime DATETIME NULL,
    updateUser VARCHAR(64) NULL,
    updateTime DATETIME NULL
);

-- transaction extensions (idempotent)
SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'transaction' AND COLUMN_NAME = 'txn_kind') = 0,
    'ALTER TABLE transaction ADD COLUMN txn_kind VARCHAR(16) NULL',
    'SELECT ''txn_kind exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'transaction' AND COLUMN_NAME = 'transfer_group_id') = 0,
    'ALTER TABLE transaction ADD COLUMN transfer_group_id VARCHAR(64) NULL',
    'SELECT ''transfer_group_id exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT TABLE_NAME AS created_table
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME IN (
    'financial_account', 'account_balance_snapshot', 'transfer_pair', 'transaction_link',
    'budget', 'budget_line', 'bill', 'financial_goal'
  )
ORDER BY TABLE_NAME;
