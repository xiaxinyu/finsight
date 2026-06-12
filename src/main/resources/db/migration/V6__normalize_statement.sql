-- Import domain: normalize statement table and rename staging table.
-- Fresh databases (V0 only) may not have legacy `statement` yet — bootstrap before normalize.

CREATE TABLE IF NOT EXISTS `statement` (
  `version` int NOT NULL DEFAULT 0,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `id` varchar(255) NOT NULL,
  `file_name` varchar(512) DEFAULT NULL,
  `row_count` int DEFAULT NULL,
  `content` longtext,
  `status` varchar(32) DEFAULT NULL,
  `source_bank_code` varchar(32) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL finsight_rename_column_if_exists('statement', 'VERSION', 'version', 'INT NOT NULL DEFAULT 0');
CALL finsight_rename_column_if_exists('statement', 'CREATEUSER', 'created_by', 'VARCHAR(255) NULL');
CALL finsight_rename_column_if_exists('statement', 'CREATETIME', 'created_at', 'DATETIME(6) NULL');
CALL finsight_rename_column_if_exists('statement', 'UPDATEUSER', 'updated_by', 'VARCHAR(255) NULL');
CALL finsight_rename_column_if_exists('statement', 'UPDATETIME', 'updated_at', 'DATETIME(6) NULL');
CALL finsight_rename_column_if_exists('statement', 'ID', 'id', 'VARCHAR(255) NOT NULL');
CALL finsight_rename_column_if_exists('statement', 'BILL_FILE_NAME', 'file_name', 'VARCHAR(512) NULL');
CALL finsight_rename_column_if_exists('statement', 'bill_file_name', 'file_name', 'VARCHAR(512) NULL');
CALL finsight_rename_column_if_exists('statement', 'BILL_DATA', 'content', 'LONGTEXT NULL');
CALL finsight_rename_column_if_exists('statement', 'bill_data', 'content', 'LONGTEXT NULL');
CALL finsight_rename_column_if_exists('statement', 'BILL_ITEMS_NUMBER', 'row_count', 'INT NULL');

DROP PROCEDURE IF EXISTS finsight_statement_columns;
DELIMITER $$
CREATE PROCEDURE finsight_statement_columns()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'statement') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'statement' AND COLUMN_NAME = 'status') THEN
            ALTER TABLE statement ADD COLUMN status VARCHAR(32) NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'statement' AND COLUMN_NAME = 'source_bank_code') THEN
            ALTER TABLE statement ADD COLUMN source_bank_code VARCHAR(32) NULL;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'statement' AND COLUMN_NAME = 'deleted') THEN
            ALTER TABLE statement ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0;
        END IF;
        UPDATE statement SET status = 'COMMITTED' WHERE status IS NULL;
        UPDATE statement SET deleted = 0 WHERE deleted IS NULL;
    END IF;
END$$
DELIMITER ;
CALL finsight_statement_columns();
DROP PROCEDURE finsight_statement_columns;

-- Staging: imp_staging_entry (was transaction_temp)
DROP PROCEDURE IF EXISTS finsight_migrate_staging;
DELIMITER $$
CREATE PROCEDURE finsight_migrate_staging()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction_temp') THEN
        RENAME TABLE transaction_temp TO imp_staging_entry;
    END IF;
END$$
DELIMITER ;
CALL finsight_migrate_staging();
DROP PROCEDURE finsight_migrate_staging;

DROP PROCEDURE IF EXISTS finsight_staging_view;
DELIMITER $$
CREATE PROCEDURE finsight_staging_view()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'imp_staging_entry') THEN
        CREATE OR REPLACE VIEW transaction_temp AS SELECT * FROM imp_staging_entry;
    END IF;
END$$
DELIMITER ;
CALL finsight_staging_view();
DROP PROCEDURE finsight_staging_view;
