-- Bank accounts: fin_bank_account is canonical; bank_card view for transition.

CREATE TABLE IF NOT EXISTS fin_bank_account (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    bank_code       VARCHAR(32)  NULL,
    card_type_code  VARCHAR(32)  NULL,
    card_no         VARCHAR(64)  NULL,
    card_name       VARCHAR(128) NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)  NULL,
    created_at      DATETIME(3)  NULL,
    updated_by      VARCHAR(64)  NULL,
    updated_at      DATETIME(3)  NULL,
    KEY idx_fin_bank_account_bank (bank_code, card_type_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS finsight_copy_bank_cards;
DELIMITER $$
CREATE PROCEDURE finsight_copy_bank_cards()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card') THEN
        INSERT INTO fin_bank_account (id, bank_code, card_type_code, card_no, card_name, deleted, created_by, created_at, updated_by, updated_at)
        SELECT bc.id, bc.bank_code, bc.card_type_code, bc.card_no, bc.card_name, COALESCE(bc.deleted, 0),
               bc.createUser, bc.createTime, bc.updateUser, bc.updateTime
        FROM bank_card bc
        WHERE NOT EXISTS (SELECT 1 FROM fin_bank_account f WHERE f.id = bc.id);
    END IF;
END$$
DELIMITER ;
CALL finsight_copy_bank_cards();
DROP PROCEDURE finsight_copy_bank_cards;

CALL finsight_rename_column_if_exists('fin_bank_account', 'createUser', 'created_by', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('fin_bank_account', 'createTime', 'created_at', 'DATETIME(3) NULL');
CALL finsight_rename_column_if_exists('fin_bank_account', 'updateUser', 'updated_by', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('fin_bank_account', 'updateTime', 'updated_at', 'DATETIME(3) NULL');

DROP PROCEDURE IF EXISTS finsight_deprecate_bank_card_table;
DELIMITER $$
CREATE PROCEDURE finsight_deprecate_bank_card_table()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_card' AND TABLE_TYPE = 'BASE TABLE'
    ) THEN
        RENAME TABLE bank_card TO _deprecated_bank_card;
    END IF;
END$$
DELIMITER ;
CALL finsight_deprecate_bank_card_table();
DROP PROCEDURE finsight_deprecate_bank_card_table;

CREATE OR REPLACE VIEW bank_card AS
SELECT id, bank_code, card_type_code, card_no, card_name, deleted,
       created_by AS createUser, created_at AS createTime, updated_by AS updateUser, updated_at AS updateTime
FROM fin_bank_account;

DROP PROCEDURE IF EXISTS finsight_link_card_ids;
DELIMITER $$
CREATE PROCEDURE finsight_link_card_ids()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'card')
       AND EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction' AND COLUMN_NAME = 'card_id') THEN
        UPDATE transaction t
        INNER JOIN card c ON c.CARD_ID = t.card_id
        INNER JOIN fin_bank_account f ON f.id = c.CARD_ID
        SET t.bank_card_id = f.id
        WHERE (t.bank_card_id IS NULL OR t.bank_card_id = '')
          AND t.card_id IS NOT NULL AND t.card_id != '';
    END IF;
END$$
DELIMITER ;
CALL finsight_link_card_ids();
DROP PROCEDURE finsight_link_card_ids;
