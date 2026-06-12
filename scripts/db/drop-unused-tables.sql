-- 删除 FinSight 不再使用的表/视图（手动执行，不在应用启动时跑）
-- mysql -u root -p finsight < scripts/db/drop-unused-tables.sql

USE finsight;

-- 归档 benefit
DROP TABLE IF EXISTS `_deprecated_medical`;
DROP TABLE IF EXISTS `_deprecated_endowment`;
DROP TABLE IF EXISTS `_deprecated_accumulation`;
DROP TABLE IF EXISTS `_deprecated_unemployment`;
DROP TABLE IF EXISTS `_deprecated_bank_card`;
DROP TABLE IF EXISTS `medical`;
DROP TABLE IF EXISTS `endowment`;
DROP TABLE IF EXISTS `accumulation`;
DROP TABLE IF EXISTS `unemployment`;

-- cls 重复归档（可能是 VIEW 或 TABLE）
DROP VIEW IF EXISTS `_archive_consume_rule_tag`;
DROP TABLE IF EXISTS `_archive_consume_rule_tag`;
DROP VIEW IF EXISTS `_archive_consume_rule`;
DROP TABLE IF EXISTS `_archive_consume_rule`;
DROP VIEW IF EXISTS `_archive_consume_category`;
DROP TABLE IF EXISTS `_archive_consume_category`;

-- card 遗留
DROP VIEW IF EXISTS `card`;
DROP TABLE IF EXISTS `_archive_card_legacy`;

-- planning / 链接
DROP TABLE IF EXISTS `budget_line`;
DROP TABLE IF EXISTS `budget`;
DROP TABLE IF EXISTS `bill`;
DROP TABLE IF EXISTS `financial_goal`;
DROP TABLE IF EXISTS `account_balance_snapshot`;
DROP TABLE IF EXISTS `_archive_financial_account`;
DROP TABLE IF EXISTS `financial_account`;
DROP TABLE IF EXISTS `transfer_pair`;
DROP TABLE IF EXISTS `transaction_link`;

-- Django（FinSight 用 fs_user）
DROP TABLE IF EXISTS `django_admin_log`;
DROP TABLE IF EXISTS `django_session`;
DROP TABLE IF EXISTS `auth_user_user_permissions`;
DROP TABLE IF EXISTS `auth_user_groups`;
DROP TABLE IF EXISTS `auth_group_permissions`;
DROP TABLE IF EXISTS `auth_user`;
DROP TABLE IF EXISTS `auth_group`;
DROP TABLE IF EXISTS `auth_permission`;
DROP TABLE IF EXISTS `django_content_type`;
DROP TABLE IF EXISTS `django_migrations`;

-- 其他历史
DROP TABLE IF EXISTS `deposit_record`;
DROP TABLE IF EXISTS `deposit`;
DROP TABLE IF EXISTS `CREDIT`;
DROP TABLE IF EXISTS `salary`;
DROP TABLE IF EXISTS `consume_rule_tag`;
DROP TABLE IF EXISTS `consume_rule`;
DROP TABLE IF EXISTS `consume_category`;

-- 删后剩余表一览
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;
