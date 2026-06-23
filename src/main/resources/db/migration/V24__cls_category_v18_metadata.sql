-- V24: Category metadata for v1.8 report semantics (schema only — no data backfill).

SET @col_budgetable := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cls_category' AND COLUMN_NAME = 'budgetable'
);
SET @ddl_budgetable := IF(@col_budgetable = 0,
    'ALTER TABLE cls_category ADD COLUMN budgetable TINYINT(1) NULL DEFAULT 1 AFTER report_role',
    'SELECT 1');
PREPARE stmt FROM @ddl_budgetable; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_cashflow := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cls_category' AND COLUMN_NAME = 'cashflow_impact'
);
SET @ddl_cashflow := IF(@col_cashflow = 0,
    'ALTER TABLE cls_category ADD COLUMN cashflow_impact VARCHAR(32) NULL AFTER budgetable',
    'SELECT 1');
PREPARE stmt FROM @ddl_cashflow; EXECUTE stmt; DEALLOCATE PREPARE stmt;
