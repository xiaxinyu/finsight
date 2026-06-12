-- Benefits: merge medical, endowment, accumulation, unemployment into ben_contribution.

CREATE TABLE IF NOT EXISTS ben_contribution (
    id                VARCHAR(255)   NOT NULL PRIMARY KEY,
    benefit_type      VARCHAR(32)    NOT NULL,
    unit_no           VARCHAR(64)    NULL,
    unit_name         VARCHAR(256)   NULL,
    period_label      VARCHAR(16)    NULL,
    pay_base          DECIMAL(19,4)  NULL,
    personal_pay      DECIMAL(19,4)  NULL,
    unit_pay          DECIMAL(19,4)  NULL,
    total_pay         DECIMAL(19,4)  NULL,
    personal_reserved DECIMAL(19,4)  NULL,
    memo              TEXT           NULL,
    fiscal_year       SMALLINT       NULL,
    created_by        VARCHAR(255)   NULL,
    created_at        DATETIME(6)    NULL,
    updated_by        VARCHAR(255)   NULL,
    updated_at        DATETIME(6)    NULL,
    version           INT            NOT NULL DEFAULT 0,
    deleted           TINYINT(1)     NOT NULL DEFAULT 0,
    KEY idx_ben_type_year (benefit_type, fiscal_year, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Partial V7 runs may have created id as VARCHAR(36); widen before copy.
ALTER TABLE ben_contribution MODIFY COLUMN id VARCHAR(255) NOT NULL;

DROP PROCEDURE IF EXISTS finsight_copy_benefit;
DELIMITER $$
CREATE PROCEDURE finsight_copy_benefit(IN p_type VARCHAR(32), IN p_table VARCHAR(64))
BEGIN
    SET @sql = CONCAT(
        'INSERT INTO ben_contribution (id, benefit_type, unit_no, unit_name, period_label, pay_base, personal_pay, unit_pay, total_pay, personal_reserved, memo, created_by, created_at, updated_by, updated_at, version) ',
        'SELECT ID, ''', p_type, ''', UNIT_NO, UNIT_NAME, `TIME`, PAY_BASE, PERSONAL_PAY, UNIT_PAY, TOTAL_PAY, PERSONAL_RESERVED, DEMOAREA, CREATEUSER, CREATETIME, UPDATEUSER, UPDATETIME, COALESCE(VERSION,0) ',
        'FROM ', p_table, ' b WHERE NOT EXISTS (SELECT 1 FROM ben_contribution x WHERE x.id = b.ID)'
    );
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table) THEN
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL finsight_copy_benefit('MEDICAL', 'medical');
CALL finsight_copy_benefit('ENDOWMENT', 'endowment');
CALL finsight_copy_benefit('ACCUMULATION', 'accumulation');
CALL finsight_copy_benefit('UNEMPLOYMENT', 'unemployment');
DROP PROCEDURE finsight_copy_benefit;

DROP PROCEDURE IF EXISTS finsight_deprecate_benefit_tables;
DELIMITER $$
CREATE PROCEDURE finsight_deprecate_benefit_tables()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical' AND TABLE_TYPE = 'BASE TABLE') THEN
        RENAME TABLE medical TO _deprecated_medical;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'endowment' AND TABLE_TYPE = 'BASE TABLE') THEN
        RENAME TABLE endowment TO _deprecated_endowment;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'accumulation' AND TABLE_TYPE = 'BASE TABLE') THEN
        RENAME TABLE accumulation TO _deprecated_accumulation;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'unemployment' AND TABLE_TYPE = 'BASE TABLE') THEN
        RENAME TABLE unemployment TO _deprecated_unemployment;
    END IF;
END$$
DELIMITER ;
CALL finsight_deprecate_benefit_tables();
DROP PROCEDURE finsight_deprecate_benefit_tables;
