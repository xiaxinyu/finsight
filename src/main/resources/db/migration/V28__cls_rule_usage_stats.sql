-- V28: Rule usage stats columns (schema only — populated by future jobs).

SET @col_last_matched := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cls_rule' AND COLUMN_NAME = 'last_matched_at'
);
SET @ddl_last_matched := IF(@col_last_matched = 0,
    'ALTER TABLE cls_rule ADD COLUMN last_matched_at DATETIME NULL AFTER end_date',
    'SELECT 1');
PREPARE stmt FROM @ddl_last_matched; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_hit_count := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cls_rule' AND COLUMN_NAME = 'hit_count'
);
SET @ddl_hit_count := IF(@col_hit_count = 0,
    'ALTER TABLE cls_rule ADD COLUMN hit_count INT NULL DEFAULT 0 AFTER last_matched_at',
    'SELECT 1');
PREPARE stmt FROM @ddl_hit_count; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_impact := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cls_rule' AND COLUMN_NAME = 'impact_amount_90d'
);
SET @ddl_impact := IF(@col_impact = 0,
    'ALTER TABLE cls_rule ADD COLUMN impact_amount_90d DECIMAL(18,2) NULL AFTER hit_count',
    'SELECT 1');
PREPARE stmt FROM @ddl_impact; EXECUTE stmt; DEALLOCATE PREPARE stmt;
