-- Legacy card table → archive + compatibility VIEW over fin_bank_account (no data dropped).

SET @db := DATABASE();

SET @card_is_table := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = @db AND table_name = 'card' AND table_type = 'BASE TABLE'
);
SET @archive_missing := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = @db AND table_name = '_archive_card_legacy'
);

SET @sql := IF(@card_is_table > 0 AND @archive_missing = 0,
    'RENAME TABLE `card` TO `_archive_card_legacy`',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @view_missing := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = @db AND table_name = 'card' AND table_type = 'VIEW'
);

SET @sql := IF(@view_missing = 0,
    'CREATE VIEW `card` AS SELECT 0 AS VERSION, created_by AS CREATEUSER, created_at AS CREATETIME, updated_by AS UPDATEUSER, updated_at AS UPDATETIME, id AS CARD_ID, card_name AS CARD_NAME FROM fin_bank_account WHERE COALESCE(deleted, 0) = 0',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
