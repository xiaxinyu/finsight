-- =============================================================================
-- FinSight 无用表清理（手动执行，不会在应用启动时自动跑）
--
-- 用法：
--   mysql -u root -p finsight < scripts/db/cleanup-unused-tables.sql
--
-- 或只跑「盘点」段：复制 STEP 1 到 MySQL 客户端执行。
-- 确认 STEP 1 结果后再取消 STEP 2 里对应块的注释并执行。
-- =============================================================================

USE finsight;

-- -----------------------------------------------------------------------------
-- STEP 1 — 盘点（只读）：哪些表存在、多少行、建议
-- -----------------------------------------------------------------------------
SELECT
    t.table_name AS `table`,
    t.table_type AS `type`,
    COALESCE(t.table_rows, 0) AS approx_rows,
    CASE c.category
        WHEN 'core'       THEN 'KEEP — 应用在用'
        WHEN 'safe_drop'  THEN 'CAN DROP — 无 Java 读写路径'
        WHEN 'review'     THEN 'REVIEW — 确认无数据需求后再删'
        ELSE 'UNKNOWN'
    END AS recommendation,
    c.note AS reason
FROM information_schema.tables t
JOIN (
    SELECT 'transaction' AS table_name, 'core' AS category, '账本' AS note
    UNION SELECT 'imp_staging_entry', 'core', '导入暂存'
    UNION SELECT 'statement', 'core', '对账单文件'
    UNION SELECT 'fin_bank_account', 'core', '银行卡（主表）'
    UNION SELECT 'bank_card', 'core', 'fin_bank_account 兼容视图'
    UNION SELECT 'cls_category', 'core', '分类'
    UNION SELECT 'cls_rule', 'core', '规则'
    UNION SELECT 'cls_rule_tag', 'core', '规则标签'
    UNION SELECT 'fs_user', 'core', '登录用户'
    UNION SELECT 'fs_role', 'core', '角色'
    UNION SELECT 'fs_user_role', 'core', '用户角色'
    UNION SELECT 'ben_contribution', 'core', '社保（当前写入表）'
    UNION SELECT 'house_rent', 'review', '租房 API 仍保留，删前确认不用'
    UNION SELECT 'flyway_schema_history', 'core', 'Flyway 版本记录'
    UNION SELECT 'transaction_temp', 'core', 'imp_staging_entry 视图'
    -- 可安全删除（应用不引用）
    UNION SELECT '_deprecated_medical', 'safe_drop', '已迁到 ben_contribution 的归档副本'
    UNION SELECT '_deprecated_endowment', 'safe_drop', '同上'
    UNION SELECT '_deprecated_accumulation', 'safe_drop', '同上'
    UNION SELECT '_deprecated_unemployment', 'safe_drop', '同上'
    UNION SELECT '_deprecated_bank_card', 'safe_drop', '同上'
    UNION SELECT 'medical', 'safe_drop', '旧表名，mapper 已写 ben_contribution'
    UNION SELECT 'endowment', 'safe_drop', '同上'
    UNION SELECT 'accumulation', 'safe_drop', '同上'
    UNION SELECT 'unemployment', 'safe_drop', '同上'
    UNION SELECT '_archive_consume_category', 'safe_drop', 'cls_category 重复归档'
    UNION SELECT '_archive_consume_rule', 'safe_drop', 'cls_rule 重复归档'
    UNION SELECT '_archive_consume_rule_tag', 'safe_drop', 'cls_rule_tag 重复归档'
    UNION SELECT '_archive_card_legacy', 'safe_drop', '旧 card 表归档，现用 fin_bank_account'
    UNION SELECT '_archive_financial_account', 'safe_drop', '规划模块未用 DB'
    UNION SELECT 'card', 'safe_drop', '兼容视图，Java 已不用 Card 实体'
    UNION SELECT 'auth_user', 'safe_drop', 'Django 遗留，现用 fs_user'
    UNION SELECT 'auth_group', 'safe_drop', 'Django 遗留'
    UNION SELECT 'auth_permission', 'safe_drop', 'Django 遗留'
    UNION SELECT 'auth_group_permissions', 'safe_drop', 'Django 遗留'
    UNION SELECT 'auth_user_groups', 'safe_drop', 'Django 遗留'
    UNION SELECT 'auth_user_user_permissions', 'safe_drop', 'Django 遗留'
    UNION SELECT 'django_content_type', 'safe_drop', 'Django 遗留'
    UNION SELECT 'django_migrations', 'safe_drop', 'Django 遗留'
    UNION SELECT 'django_session', 'safe_drop', 'Django 遗留'
    UNION SELECT 'django_admin_log', 'safe_drop', 'Django 遗留'
    UNION SELECT 'deposit', 'safe_drop', '历史表，无 mapper'
    UNION SELECT 'deposit_record', 'safe_drop', '历史表，无 mapper'
    UNION SELECT 'CREDIT', 'safe_drop', '历史表，无 mapper'
    UNION SELECT 'transfer_pair', 'safe_drop', '死代码，V9 已处理空表'
    UNION SELECT 'transaction_link', 'safe_drop', '死代码'
    UNION SELECT 'budget', 'safe_drop', 'Planning 走内存'
    UNION SELECT 'budget_line', 'safe_drop', 'Planning 走内存'
    UNION SELECT 'bill', 'safe_drop', 'Planning 走内存'
    UNION SELECT 'financial_goal', 'safe_drop', 'Planning 走内存'
    UNION SELECT 'financial_account', 'safe_drop', '无 Java 注入'
    UNION SELECT 'account_balance_snapshot', 'safe_drop', '无 Java 注入'
    UNION SELECT 'consume_category', 'review', '若与 cls_category 行数一致可归档后删'
    UNION SELECT 'consume_rule', 'review', '若与 cls_rule 行数一致可归档后删'
    UNION SELECT 'consume_rule_tag', 'review', '若与 cls_rule_tag 一致可删'
    UNION SELECT 'salary', 'review', '旧工资表，收入已走 transaction'
) c ON c.table_name = t.table_name
WHERE t.table_schema = DATABASE()
ORDER BY
    FIELD(c.category, 'core', 'review', 'safe_drop', 'UNKNOWN'),
    t.table_name;

-- 核对：ben_contribution 是否已有 benefit 数据（删 _deprecated_* 前看一眼）
SELECT benefit_type, COUNT(*) AS rows_cnt
FROM ben_contribution
GROUP BY benefit_type
ORDER BY benefit_type;

-- -----------------------------------------------------------------------------
-- STEP 2 — 删除（默认全部注释；确认 STEP 1 后按需取消注释）
-- 建议顺序：先删子表/视图，再删父表
-- -----------------------------------------------------------------------------

-- /* === A. 归档 benefit（确认 ben_contribution 有数据后再删）===
-- DROP TABLE IF EXISTS `_deprecated_medical`;
-- DROP TABLE IF EXISTS `_deprecated_endowment`;
-- DROP TABLE IF EXISTS `_deprecated_accumulation`;
-- DROP TABLE IF EXISTS `_deprecated_unemployment`;
-- DROP TABLE IF EXISTS `_deprecated_bank_card`;
-- DROP TABLE IF EXISTS `medical`;
-- DROP TABLE IF EXISTS `endowment`;
-- DROP TABLE IF EXISTS `accumulation`;
-- DROP TABLE IF EXISTS `unemployment`;
-- */

-- /* === B. cls 重复归档 ===
-- DROP TABLE IF EXISTS `_archive_consume_rule_tag`;
-- DROP TABLE IF EXISTS `_archive_consume_rule`;
-- DROP TABLE IF EXISTS `_archive_consume_category`;
-- */

-- /* === C. card 遗留（应用用 fin_bank_account）===
-- DROP VIEW IF EXISTS `card`;
-- DROP TABLE IF EXISTS `_archive_card_legacy`;
-- */

-- /* === D. Planning / 链接死表 ===
-- DROP TABLE IF EXISTS `budget_line`;
-- DROP TABLE IF EXISTS `budget`;
-- DROP TABLE IF EXISTS `bill`;
-- DROP TABLE IF EXISTS `financial_goal`;
-- DROP TABLE IF EXISTS `account_balance_snapshot`;
-- DROP TABLE IF EXISTS `_archive_financial_account`;
-- DROP TABLE IF EXISTS `financial_account`;
-- DROP TABLE IF EXISTS `transfer_pair`;
-- DROP TABLE IF EXISTS `transaction_link`;
-- */

-- /* === E. Django 遗留（确认不用 Django 管理后台）===
-- DROP TABLE IF EXISTS `django_admin_log`;
-- DROP TABLE IF EXISTS `django_session`;
-- DROP TABLE IF EXISTS `auth_user_user_permissions`;
-- DROP TABLE IF EXISTS `auth_user_groups`;
-- DROP TABLE IF EXISTS `auth_user`;
-- DROP TABLE IF EXISTS `auth_group_permissions`;
-- DROP TABLE IF EXISTS `auth_group`;
-- DROP TABLE IF EXISTS `auth_permission`;
-- DROP TABLE IF EXISTS `django_content_type`;
-- DROP TABLE IF EXISTS `django_migrations`;
-- */

-- /* === F. 其他历史表 ===
-- DROP TABLE IF EXISTS `deposit_record`;
-- DROP TABLE IF EXISTS `deposit`;
-- DROP TABLE IF EXISTS `CREDIT`;
-- */

-- -----------------------------------------------------------------------------
-- STEP 3 — 删后再跑一遍盘点，leftover 应为空（除 KEEP / REVIEW 你保留的）
-- -----------------------------------------------------------------------------
