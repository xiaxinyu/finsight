-- v2.0.0: transaction indexes for profile/forecast concentration hot paths
-- Safe to re-run: uses information_schema guards

SET @db := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = @db AND table_name = 'transaction' AND index_name = 'idx_txn_owner_deleted_date') = 0,
    'CREATE INDEX idx_txn_owner_deleted_date ON transaction (created_by, deleted, transaction_date)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = @db AND table_name = 'transaction' AND index_name = 'idx_txn_consume_code') = 0,
    'CREATE INDEX idx_txn_consume_code ON transaction (consume_code)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = @db AND table_name = 'transaction' AND index_name = 'idx_txn_kind') = 0,
    'CREATE INDEX idx_txn_kind ON transaction (txn_kind)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = @db AND table_name = 'transaction' AND index_name = 'idx_txn_bank_card') = 0,
    'CREATE INDEX idx_txn_bank_card ON transaction (bank_card_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
