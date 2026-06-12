-- Add canonical category column names alongside legacy consume_* (dual-column transition).

SET @db := DATABASE();

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'transaction' AND column_name = 'category_id');
SET @sql := IF(@exists = 0,
    'ALTER TABLE `transaction` ADD COLUMN `category_id` varchar(255) DEFAULT NULL',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'transaction' AND column_name = 'category_code');
SET @sql := IF(@exists = 0,
    'ALTER TABLE `transaction` ADD COLUMN `category_code` varchar(64) DEFAULT NULL',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'transaction' AND column_name = 'category_name');
SET @sql := IF(@exists = 0,
    'ALTER TABLE `transaction` ADD COLUMN `category_name` varchar(255) DEFAULT NULL',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'imp_staging_entry' AND column_name = 'category_id');
SET @sql := IF(@exists = 0,
    'ALTER TABLE `imp_staging_entry` ADD COLUMN `category_id` varchar(255) DEFAULT NULL',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'imp_staging_entry' AND column_name = 'category_code');
SET @sql := IF(@exists = 0,
    'ALTER TABLE `imp_staging_entry` ADD COLUMN `category_code` varchar(64) DEFAULT NULL',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'imp_staging_entry' AND column_name = 'category_name');
SET @sql := IF(@exists = 0,
    'ALTER TABLE `imp_staging_entry` ADD COLUMN `category_name` varchar(255) DEFAULT NULL',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE `transaction`
SET category_id = COALESCE(category_id, consume_id),
    category_code = COALESCE(category_code, consume_code),
    category_name = COALESCE(category_name, consume_name)
WHERE (category_id IS NULL AND consume_id IS NOT NULL)
   OR (category_code IS NULL AND consume_code IS NOT NULL)
   OR (category_name IS NULL AND consume_name IS NOT NULL);

UPDATE `imp_staging_entry`
SET category_id = COALESCE(category_id, consume_id),
    category_code = COALESCE(category_code, consume_code),
    category_name = COALESCE(category_name, consume_name)
WHERE (category_id IS NULL AND consume_id IS NOT NULL)
   OR (category_code IS NULL AND consume_code IS NOT NULL)
   OR (category_name IS NULL AND consume_name IS NOT NULL);
