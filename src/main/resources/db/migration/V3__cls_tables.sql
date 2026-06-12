-- Classification domain: rename consume_* to cls_* and normalize column names.

DROP PROCEDURE IF EXISTS finsight_migrate_cls;
DELIMITER $$
CREATE PROCEDURE finsight_migrate_cls()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consume_category') THEN
        RENAME TABLE consume_category TO cls_category;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consume_rule') THEN
        RENAME TABLE consume_rule TO cls_rule;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consume_rule_tag') THEN
        RENAME TABLE consume_rule_tag TO cls_rule_tag;
    END IF;
END$$
DELIMITER ;
CALL finsight_migrate_cls();
DROP PROCEDURE finsight_migrate_cls;

CALL finsight_rename_column_if_exists('cls_category', 'parentId', 'parent_id', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('cls_category', 'sortNo', 'sort_no', 'INT NULL');
CALL finsight_rename_column_if_exists('cls_category', 'createTime', 'created_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('cls_category', 'updateTime', 'updated_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('cls_category', 'createUser', 'created_by', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('cls_category', 'updateUser', 'updated_by', 'VARCHAR(64) NULL');

CALL finsight_rename_column_if_exists('cls_rule', 'categoryId', 'category_id', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'patternType', 'pattern_type', 'VARCHAR(32) NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'bankCode', 'bank_code', 'VARCHAR(32) NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'cardTypeCode', 'card_type_code', 'VARCHAR(32) NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'minAmount', 'min_amount', 'DECIMAL(12,2) NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'maxAmount', 'max_amount', 'DECIMAL(12,2) NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'startDate', 'start_date', 'DATE NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'endDate', 'end_date', 'DATE NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'createTime', 'created_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'updateTime', 'updated_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'createUser', 'created_by', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('cls_rule', 'updateUser', 'updated_by', 'VARCHAR(64) NULL');

DROP PROCEDURE IF EXISTS finsight_cls_compat_views;
DELIMITER $$
CREATE PROCEDURE finsight_cls_compat_views()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cls_category') THEN
        CREATE OR REPLACE VIEW consume_category AS SELECT * FROM cls_category;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cls_rule') THEN
        CREATE OR REPLACE VIEW consume_rule AS SELECT * FROM cls_rule;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cls_rule_tag') THEN
        CREATE OR REPLACE VIEW consume_rule_tag AS SELECT * FROM cls_rule_tag;
    END IF;
END$$
DELIMITER ;
CALL finsight_cls_compat_views();
DROP PROCEDURE finsight_cls_compat_views;
