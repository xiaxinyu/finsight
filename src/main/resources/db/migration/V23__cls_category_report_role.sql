-- V23: cls_category.report_role for v1.8 report semantics (schema only — no data backfill).
-- Data backfill: docs/tech/database/l2-category-sprint2-seed.sql (manual).

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cls_category'
      AND COLUMN_NAME = 'report_role'
);

SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE cls_category ADD COLUMN report_role VARCHAR(32) NULL AFTER txn_types',
    'SELECT 1');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
