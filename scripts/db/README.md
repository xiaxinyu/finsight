# 数据库手动脚本

应用启动时 **不会** 执行这些脚本（Flyway 只管 `src/main/resources/db/migration/` 里的结构迁移）。

| 脚本 | 用途 |
|------|------|
| [`cleanup-unused-tables.sql`](cleanup-unused-tables.sql) | **STEP 1** 盘点哪些表能删 |
| [`drop-unused-tables.sql`](drop-unused-tables.sql) / [`drop-unused-tables.sh`](drop-unused-tables.sh) | **直接删除**无效表（不在启动时执行） |
| [`preview-duplicate-transactions.sql`](preview-duplicate-transactions.sql) | 查看跨账单重复导入（先跑） |
| [`soft-delete-duplicate-transactions.sql`](soft-delete-duplicate-transactions.sql) | 软删跨账单重复导入 |
| [`preview-cmb-duplicate-transactions.sql`](preview-cmb-duplicate-transactions.sql) | 查看招商银行重复（描述可略有差异） |
| [`soft-delete-cmb-duplicate-transactions.sql`](soft-delete-cmb-duplicate-transactions.sql) | 软删招商银行重复 |
| Flyway `V18__transaction_analytics_view.sql` | 分析宽表视图 `v_transaction_analytics` |
| [`export-classification-audit-baseline.sh`](export-classification-audit-baseline.sh) | 导出分类审计 baseline CSV/JSON 到 `docs/tech/database/audit-results/` |
| [`l2-category-sprint2-seed.sql`](../docs/tech/database/l2-category-sprint2-seed.sql) | **手动** 插入 v1.8 L2 分类（Issue #69，不批量改历史交易） |
| `GET /api/v1/maintenance/l2-category-seed-plan` | 在线 L2 seed 计划（待插入 / 已存在） |
| `GET /api/v1/maintenance/classification-audit-summary` | 在线汇总 + P0/P1/P2 remediation plan |
| `GET /api/v1/analytics/export?format=csv` | 导出 CSV 供 Excel / Python 挖掘 |

## 推荐流程

```bash
# 1. 只盘点（把 STEP 1 两段 SELECT 复制到 MySQL 客户端，或整文件执行——STEP 2 已注释不会删）
mysql -u root -p finsight < scripts/db/cleanup-unused-tables.sql

# 2. 确认 ben_contribution 有数据、CAN DROP 表无保留价值后，编辑脚本取消 STEP 2 对应块注释再执行
```

删完后可用 API 核对：`POST /api/v1/maintenance/verify-schema-migration` → `leftoverLegacyTables` 应为 `[]`。

## Flyway 启动失败（failed migration）

若日志出现 `Detected failed migration to version N`：

```bash
# 清除失败记录（例：V17）
./scripts/db/repair-flyway.sh 17

# 或在 MySQL 里执行
# scripts/db/flyway-repair-failed-v17.sql
```

然后重新 `mvn spring-boot:run`。
