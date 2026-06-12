-- Auth domain: rename app_* to fs_* (avoids Django legacy auth_user table name clash).

DROP PROCEDURE IF EXISTS finsight_migrate_auth;
DELIMITER $$
CREATE PROCEDURE finsight_migrate_auth()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user') THEN
        RENAME TABLE app_user TO fs_user;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_role') THEN
        RENAME TABLE app_role TO fs_role;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'app_user_role') THEN
        RENAME TABLE app_user_role TO fs_user_role;
    END IF;
END$$
DELIMITER ;
CALL finsight_migrate_auth();
DROP PROCEDURE finsight_migrate_auth;

CALL finsight_rename_column_if_exists('fs_user', 'createTime', 'created_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('fs_user', 'updateTime', 'updated_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('fs_user', 'createUser', 'created_by', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('fs_user', 'updateUser', 'updated_by', 'VARCHAR(64) NULL');

CALL finsight_rename_column_if_exists('fs_role', 'createTime', 'created_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('fs_role', 'updateTime', 'updated_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('fs_role', 'createUser', 'created_by', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('fs_role', 'updateUser', 'updated_by', 'VARCHAR(64) NULL');

CALL finsight_rename_column_if_exists('fs_user_role', 'createTime', 'created_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('fs_user_role', 'updateTime', 'updated_at', 'DATETIME NULL');
CALL finsight_rename_column_if_exists('fs_user_role', 'createUser', 'created_by', 'VARCHAR(64) NULL');
CALL finsight_rename_column_if_exists('fs_user_role', 'updateUser', 'updated_by', 'VARCHAR(64) NULL');
