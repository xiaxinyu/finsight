# v1.8 L2 分类目标字典（Issue #69）

对齐 [v1.8 数据基础版本计划 §1.3](../roadmap/v1.8-data-foundation-version-plan.zh-cn.md)。

## 约束

- **禁止修改已有 `cls_category.code`**
- 新增 L2 仅 **INSERT**（`WHERE NOT EXISTS`）
- 允许 **UPDATE name** 消除歧义
- 新分类默认仅用于**新规则匹配**，不批量改历史交易

## 机器可读目录

Java enum：`ClassificationL2TargetCatalog`（code、name、parent L1、txn_types、report_role）

L1 根节点：`ClassificationL1TargetCatalog`

## 在线 seed plan

```http
GET /api/v1/maintenance/l2-category-seed-plan
```

返回：待插入 / 已存在 / 仅文档化条目 + 建议 name 更新。

## 手动执行

```bash
# 1. 确保 Flyway V23 已应用（report_role 列）
mvn spring-boot:run

# 2. 从当前数据库重新生成 seed SQL（推荐，避免 catalog 与库内 code 不一致）
mvn test -Dtest=L2CategorySeedSqlFromDatabaseTest -Dregenerate.seed.from.db=true

# 3. 审阅并执行 seed SQL
mysql -u <user> -p finsight < docs/tech/database/l2-category-sprint2-seed.sql
```

生成逻辑会以 **库内已有 code** 为准：仅 INSERT catalog 中仍缺失的 L2；`report_role` 回填覆盖库内所有空 role 行（含 `LIVING-*`、`FE-*`、`SHOPPING-*` 等 legacy code）。

## report_role 取值

| report_role | 用途 |
|-------------|------|
| `income` | 真实收入 |
| `budget` | 预算/日常消费 |
| `cashflow` | 现金流（保险、手续费等） |
| `refund` | 报销/退款（不计入收入趋势） |
| `transfer` | 转账/账户间移动 |
| `asset` | 资产变动 |
| `liability` | 负债变动 |
| `investment` | 投资/理财 |

## 关联

- **去重合并**：[category-dedup-merge-playbook.zh-cn.md](./category-dedup-merge-playbook.zh-cn.md)（`INC` vs `INCOME` 等）
- Audit 候选：[audit-results/2026-06-23-sprint1/l2-category-candidates.zh-cn.md](./audit-results/2026-06-23-sprint1/l2-category-candidates.zh-cn.md)
- 工作流：[classification-governance-workflow.zh-cn.md](./classification-governance-workflow.zh-cn.md)
