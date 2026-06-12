-- Migrate house_rent rows into transaction (ledger single source of truth).

DROP PROCEDURE IF EXISTS finsight_migrate_house_rent;
DELIMITER $$
CREATE PROCEDURE finsight_migrate_house_rent()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'house_rent') THEN
    INSERT INTO transaction (
        id, card_id, transaction_date, transaction_desc, balance_currency, balance_money,
        card_type_id, card_type_name, deleted, memo, created_by, created_at, updated_by, updated_at, version, txn_kind
    )
    SELECT
        hr.ID,
        hr.CARD_ID,
        hr.TRANSACTION_DATE,
        hr.TRANSACTION_DESC,
        hr.BALANCE_CURRENCY,
        hr.BALANCE_MONEY,
        hr.CARD_TYPE_ID,
        hr.CARD_TYPE_NAME,
        COALESCE(hr.DELETED, 0),
        hr.DEMOAREA,
        hr.CREATEUSER,
        hr.CREATETIME,
        hr.UPDATEUSER,
        hr.UPDATETIME,
        COALESCE(hr.VERSION, 0),
        'expense'
    FROM house_rent hr
    WHERE NOT EXISTS (SELECT 1 FROM transaction t WHERE t.id = hr.ID);
    END IF;
END$$
DELIMITER ;
CALL finsight_migrate_house_rent();
DROP PROCEDURE finsight_migrate_house_rent;

CREATE OR REPLACE VIEW v_legacy_house_rent AS SELECT * FROM house_rent;
