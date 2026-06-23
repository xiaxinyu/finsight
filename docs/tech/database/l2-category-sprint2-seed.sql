-- FinSight L2 category Sprint 2 seed (Issue #69).
-- MANUAL ONLY — review before execution. Does NOT batch-update historical transactions.
--
--   mysql -u <user> -p finsight < docs/tech/database/l2-category-sprint2-seed.sql
--
-- Generated from live cls_category (266 distinct codes, 161 active rows scanned).
-- Inserts: 15 idempotent L1/L2 rows still missing from catalog.
-- report_role updates: 161 rows (inferred for empty report_role only).
-- Prerequisites: Flyway V23+ (report_role). V24+ optional (budgetable/cashflow_impact).
-- After apply: add rules in Rule Engine; do not bulk-recategorize txns without migration batch (#75).
-- Regenerate: mvn test -Dtest=L2CategorySeedSqlFromDatabaseTest -Dregenerate.seed.from.db=true

-- Generated from ClassificationL2TargetCatalog — Issue #69
-- Manual execution only; never auto-applied by Flyway.

-- Step 0: ensure L1 roots exist (insert if missing)
-- Skips duplicate L1 when canonical root already exists (INC, TRANSPORT)
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'INC', 'INC', '收入', 1, null, 10, 'income', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'INC');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'FIXED', 'FIXED', '固定支出', 1, null, 20, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'FIXED');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'LIVING', 'LIVING', '日常生活', 1, null, 30, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'LIVING');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'SHOPPING', 'SHOPPING', '购物与耐用品', 1, null, 40, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'SHOPPING');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'TRANSPORT', 'TRANSPORT', '交通与车辆', 1, null, 50, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'TRANSPORT');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'EDU', 'EDU', '教育与培训', 1, null, 55, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'EDU');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'ENT', 'ENT', '娱乐与旅行', 1, null, 60, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'ENT');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'GIFT', 'GIFT', '人情与公益', 1, null, 65, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'GIFT');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'REIM', 'REIM', '报销与返还', 1, null, 70, 'income,refund', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'REIM');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'ASSET', 'ASSET', '资产变动', 1, null, 75, 'transfer,asset', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'ASSET');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'LIABILITY', 'LIABILITY', '负债变动', 1, null, 80, 'transfer,liability', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'LIABILITY');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'INVEST', 'INVEST', '投资活动', 1, null, 85, 'expense,invest', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'INVEST');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'WEALTH', 'WEALTH', '理财与金融产品', 1, null, 90, 'invest', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'WEALTH');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'FEE', 'FEE', '金融手续费', 1, null, 92, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'FEE');
insert into cls_category (id, code, name, level, parent_id, sort_no, txn_types, deleted, version, created_at, updated_at) select 'OTHER', 'OTHER', '其它消费', 1, null, 99, 'expense', 0, 0, now(), now() from dual where not exists (select 1 from cls_category where code = 'OTHER');


-- Optional name clarifications (does NOT change code)

update cls_category set name = '临时无法归类', updated_at = now() where code = 'OTHER-01' and coalesce(deleted, 0) = 0 and name = '无法归类的支出';
update cls_category set name = '投资未分类', updated_at = now() where code = 'INVEST-OTHER' and coalesce(deleted, 0) = 0 and name = '其它消费';
update cls_category set name = '其它消费', updated_at = now() where code = 'OTHER' and coalesce(deleted, 0) = 0 and name = '其他类别' and level = 1;

-- Backfill report_role for live cls_category rows (inferred; requires V23 column)

update cls_category set report_role = 'budget' where code = 'GIFT' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIM' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'WEALTH' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'FIXED' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANSPORT' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'EDU' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'ENT' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SOCIAL' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIMB' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'asset' where code = 'ASSET' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'LIABILITY' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'INVEST' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'FP' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FE' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'OTHER' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'asset' where code = 'ASSET-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'transfer' where code = 'ASSET-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'asset' where code = 'ASSET-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'asset' where code = 'ASSET-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'asset' where code = 'ASSET-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'EDU-00' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'EDU-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'EDU-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'EDU-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'EDU-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'EDU-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'ENT-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'ENT-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'ENT-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'ENT-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'ENT-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'ENT-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'ENT-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FIN-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FEE-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FEE-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FEE-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FE-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FE-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FE-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FE-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'FIXED-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'FIXED-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'FIXED-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FIXED-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'FIXED-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'FIXED-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'FIXED-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'FIXED-08' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'FIXED-09' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'FIXED-99' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'FP-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'FP-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'FP-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'FP-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'FP-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'FP-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'GIFT-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'transfer' where code = 'GIFT-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'GIFT-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'GIFT-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INCOME-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'INC-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'INC-08' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC-09' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'INC-11' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'INC-12' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'INVEST-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'INVEST-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'INVEST-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'INVEST-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'INVEST-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'INVEST-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'DEBT-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'LIABILITY-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'DEBT-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'LIABILITY-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'DEBT-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'LIABILITY-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'DEBT-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'LIABILITY-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'DEBT-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'LIABILITY-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'liability' where code = 'LIABILITY-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'LIABILITY-08' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'DAILY-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'DAILY-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'DAILY-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-08' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-09' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-10' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-11' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-16' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-18' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'LIVING-17' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'OTHER-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'OTHER-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'OTHER-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIM-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIM-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIM-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIM-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIM-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIMB-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIMB-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIMB-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'refund' where code = 'REIMB-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOP-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOP-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOP-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOP-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOP-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-08' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SHOPPING-09' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SOCIAL-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'income' where code = 'SOCIAL-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SOCIAL-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SOCIAL-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SOCIAL-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'SOCIAL-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANSPORT-00' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANSPORT-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANS-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANS-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANSPORT-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANS-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANSPORT-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANS-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANSPORT-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'cashflow' where code = 'TRANS-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANSPORT-06' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANS-07' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'budget' where code = 'TRANSPORT-08' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'WEALTH-01' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'WEALTH-02' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'WEALTH-03' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'WEALTH-04' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
update cls_category set report_role = 'investment' where code = 'WEALTH-05' and coalesce(deleted, 0) = 0 and (report_role is null or trim(report_role) = '');
