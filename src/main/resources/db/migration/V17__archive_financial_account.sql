-- Archive financial_account when present (no-op if table never existed).
-- Uses information_schema only so missing table does not fail migration.

SET @db := DATABASE();

SET @fa_exists := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = @db AND table_name = 'financial_account' AND table_type = 'BASE TABLE'
);

SET @archive_exists := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = @db AND table_name = '_archive_financial_account'
);

SET @fa_rows := IF(@fa_exists > 0,
    (SELECT COALESCE(table_rows, 0) FROM information_schema.tables
     WHERE table_schema = @db AND table_name = 'financial_account' LIMIT 1),
    0);

SET @do_rename := IF(@fa_exists > 0 AND @archive_exists = 0 AND @fa_rows > 0, 1, 0);
SET @sql := IF(@do_rename = 1, 'RENAME TABLE financial_account TO _archive_financial_account', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @do_drop := IF(@fa_exists > 0 AND @fa_rows = 0, 1, 0);
SET @sql := IF(@do_drop = 1, 'DROP TABLE financial_account', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
