-- FinSight schema baseline (pre-Flyway databases are baselined at version 0).
-- Authoritative naming rules: docs/tech/database/schema.md

DROP PROCEDURE IF EXISTS finsight_rename_column_if_exists;
DELIMITER $$
CREATE PROCEDURE finsight_rename_column_if_exists(
    IN p_table VARCHAR(64),
    IN p_old VARCHAR(64),
    IN p_new VARCHAR(64),
    IN p_definition VARCHAR(512)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_old
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` CHANGE COLUMN `', p_old, '` `', p_new, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

SELECT 1;
