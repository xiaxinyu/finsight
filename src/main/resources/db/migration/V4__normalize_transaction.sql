-- Core ledger: keep table name `transaction`, normalize columns to snake_case.
-- All ledger data remains in `transaction` (no fin_ledger_entry rename).

DROP PROCEDURE IF EXISTS finsight_normalize_transaction_table;
DELIMITER $$
CREATE PROCEDURE finsight_normalize_transaction_table(IN p_table VARCHAR(64))
BEGIN
    CALL finsight_rename_column_if_exists(p_table, 'VERSION', 'version', 'INT NOT NULL DEFAULT 0');
    CALL finsight_rename_column_if_exists(p_table, 'CREATEUSER', 'created_by', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'createuser', 'created_by', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'CREATETIME', 'created_at', 'DATETIME(6) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'createtime', 'created_at', 'DATETIME(6) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'UPDATEUSER', 'updated_by', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'updateuser', 'updated_by', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'UPDATETIME', 'updated_at', 'DATETIME(6) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'updatetime', 'updated_at', 'DATETIME(6) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'ID', 'id', 'VARCHAR(255) NOT NULL');
    CALL finsight_rename_column_if_exists(p_table, 'CARD_ID', 'card_id', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'TRANSACTION_DATE', 'transaction_date', 'DATETIME(6) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'BOOKKEEPING_DATE', 'bookkeeping_date', 'DATETIME(6) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'TRANSACTION_DESC', 'transaction_desc', 'TEXT NULL');
    CALL finsight_rename_column_if_exists(p_table, 'BALANCE_CURRENCY', 'balance_currency', 'VARCHAR(20) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'BALANCE_MONEY', 'balance_money', 'DECIMAL(19,4) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'CARD_TYPE_ID', 'card_type_id', 'INT NULL');
    CALL finsight_rename_column_if_exists(p_table, 'CARD_TYPE_NAME', 'card_type_name', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'DELETED', 'deleted', 'INT NULL');
    CALL finsight_rename_column_if_exists(p_table, 'CONSUMPTION_TYPE', 'consumption_type', 'INT NULL');
    CALL finsight_rename_column_if_exists(p_table, 'CONSUME_ID', 'consume_id', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'CONSUME_NAME', 'consume_name', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'DEMOAREA', 'memo', 'TEXT NULL');
    CALL finsight_rename_column_if_exists(p_table, 'demoarea', 'memo', 'TEXT NULL');
    CALL finsight_rename_column_if_exists(p_table, 'demoArea', 'memo', 'TEXT NULL');
    CALL finsight_rename_column_if_exists(p_table, 'RECORDID', 'statement_id', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'recordid', 'statement_id', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'recordID', 'statement_id', 'VARCHAR(255) NULL');
    CALL finsight_rename_column_if_exists(p_table, 'PAYMENT_TYPE_ID', 'payment_type_id', 'VARCHAR(20) NULL');
END$$
DELIMITER ;

CALL finsight_normalize_transaction_table('transaction');
CALL finsight_normalize_transaction_table('transaction_temp');
DROP PROCEDURE finsight_normalize_transaction_table;

-- Normalize signed legacy expense amounts on commit path (idempotent)
UPDATE transaction
SET income_money = ABS(balance_money), balance_money = 0, txn_kind = COALESCE(NULLIF(txn_kind, ''), 'income')
WHERE COALESCE(income_money, 0) = 0 AND COALESCE(balance_money, 0) < 0;
