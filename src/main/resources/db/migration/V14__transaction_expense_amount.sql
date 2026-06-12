-- Add expense_amount alongside balance_money (dual-write transition; no data removed).

SET @db := DATABASE();

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'transaction' AND column_name = 'expense_amount');
SET @sql := IF(@exists = 0,
    'ALTER TABLE `transaction` ADD COLUMN `expense_amount` decimal(19,4) DEFAULT NULL',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'imp_staging_entry' AND column_name = 'expense_amount');
SET @sql := IF(@exists = 0,
    'ALTER TABLE `imp_staging_entry` ADD COLUMN `expense_amount` decimal(19,4) DEFAULT NULL',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE `transaction`
SET expense_amount = CASE
    WHEN balance_money IS NULL THEN NULL
    WHEN balance_money < 0 THEN ABS(balance_money)
    WHEN balance_money > 0 THEN balance_money
    ELSE 0
END
WHERE expense_amount IS NULL AND balance_money IS NOT NULL;

UPDATE `imp_staging_entry`
SET expense_amount = CASE
    WHEN balance_money IS NULL THEN NULL
    WHEN balance_money < 0 THEN ABS(balance_money)
    WHEN balance_money > 0 THEN balance_money
    ELSE 0
END
WHERE expense_amount IS NULL AND balance_money IS NOT NULL;
