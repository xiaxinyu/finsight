# v1.8 分类数据治理工作流

日期：2026-06-23  
范围：Categories、Rule Engine、交易分类字段、merchant token、报表口径。  
关联 Issue：[#67](https://github.com/xiaxinyu/finsight/issues/67)（本文档）、[#68](https://github.com/xiaxinyu/finsight/issues/68)（执行审计）、[#70](https://github.com/xiaxinyu/finsight/issues/70)（八步工作流落地）

## 原则

- **SQL 审计结果不自动修复**：所有 remediation 脚本在 `docs/tech/database/` 下，需人工审阅后手动执行。
- **禁止修改已有 `cls_category.code`**：只允许改 `name`、排序、`parent_id`、`txn_types`、`deleted` 等。
- **历史交易批量变更**须走 migration batch 审计（见 [#75](https://github.com/xiaxinyu/finsight/issues/75)），禁止无记录 ad-hoc UPDATE。
- **分类大调整前后各保存一份 audit 结果**（见 [classification-audit-baseline-template.md](./classification-audit-baseline-template.md)）。

## 八步工作流

```mermaid
flowchart LR
  A[1 Audit] --> B[2 Design]
  B --> C[3 Create]
  C --> D[4 Map Rules]
  D --> E[5 Preview]
  E --> F[6 Apply]
  F --> G[7 Refresh]
  G --> H[8 Verify]
```

| 步骤 | 名称 | 动作 | 产出 |
|------|------|------|------|
| 1 | **Audit** | 跑 `classification-data-audit.sql`，保存 baseline | `baseline-summary`、Top100、orphan/invalid 清单 |
| 2 | **Design** | 根据审计结果设计二级分类增补 / 规则修复 | 分类变更清单、规则修复清单 |
| 3 | **Create** | 在 Categories 新增分类（新 code 唯一） | 新 `cls_category` 行；手动 seed：[`l2-category-sprint2-seed.sql`](./l2-category-sprint2-seed.sql)（[#69](https://github.com/xiaxinyu/finsight/issues/69)） |
| 4 | **Map Rules** | 在 Rule Engine 新增/调整规则；orphan 用 remediation SQL 映射 | 更新 `cls_rule` |
| 5 | **Preview** | Categories impact preview ([#66](https://github.com/xiaxinyu/finsight/issues/66))；规则 dry-run ([#73](https://github.com/xiaxinyu/finsight/issues/73)) | 影响范围确认 |
| 6 | **Apply** | 人工执行 remediation SQL；UI 确认删除/合并 | DB 变更记录 |
| 7 | **Refresh** | 重算指标 / merchant profile ([#81](https://github.com/xiaxinyu/finsight/issues/81)) | `fin_metric_monthly` 等更新 |
| 8 | **Verify** | 再跑 audit + `POST /api/v1/maintenance/verify-schema-migration` | 前后 baseline 对比 |

## 1. Audit — 如何执行

```bash
mysql -u <user> -p finsight < docs/tech/database/classification-data-audit.sql > audit-$(date +%Y%m%d-%H%M).txt

# 或按章节导出 CSV/JSON 到 audit-results/<run-tag>/：
chmod +x scripts/db/export-classification-audit-baseline.sh
RUN_TAG=2026-06-23-sprint1 ./scripts/db/export-classification-audit-baseline.sh
```

必导出的 baseline 片段（对应 audit.sql 章节）：

| 章节 | 内容 | 建议文件名 |
|------|------|------------|
| §18 | 未分类 Top 100 | `baseline-unclassified-top100.csv` |
| §19 | 其它消费 Top 100 | `baseline-other-consumption-top100.csv` |
| §3–§4 | Orphan / invalid 规则清单 | `baseline-orphan-rules.csv` / `baseline-invalid-rules.csv` |
| §11 | 分类字段 drift | `baseline-field-drift.csv` |
| §20 | 汇总计数 | `baseline-summary.json` |
| §21 | Merchant token 样本 | `baseline-merchant-token-samples.csv` |

填写模板：[classification-audit-baseline-template.md](./classification-audit-baseline-template.md)

## 6. Apply — 手动 remediation 脚本

| 问题 | 脚本 | 关联 Issue |
|------|------|------------|
| Orphan rules | [orphan-rules-remediation.sql](./orphan-rules-remediation.sql) | [#63](https://github.com/xiaxinyu/finsight/issues/63) |
| Invalid / blank pattern | [invalid-rules-remediation.sql](./invalid-rules-remediation.sql) | [#64](https://github.com/xiaxinyu/finsight/issues/64) |
| 交易分类字段 drift | [transaction-category-field-remediation.sql](./transaction-category-field-remediation.sql) | [#65](https://github.com/xiaxinyu/finsight/issues/65) |
| Merchant token | [merchant-token-normalization.sql](./merchant-token-normalization.sql) | [#62](https://github.com/xiaxinyu/finsight/issues/62) |

**禁止**将这些脚本放入 Flyway 自动迁移；生产/本地均手动执行。

## 8. Verify — 验收检查

- `baseline-summary` 中 `active_orphan_rules = 0`（或均已 inactive + remark）
- `active_invalid_pattern_rules = 0`
- `category_field_drift_rows = 0`（或可解释例外）
- `unclassified_txns` / `other_category_txns` 相对 baseline 下降或已文档化
- `POST /api/v1/maintenance/verify-schema-migration` → `ok: true`

## Issue 看板（v1.8 Data Foundation）

| Issue | 标题 |
|-------|------|
| [#67](https://github.com/xiaxinyu/finsight/issues/67) | SQL 审计基线与治理工作流（本文档） |
| [#68](https://github.com/xiaxinyu/finsight/issues/68) | 执行 SQL 审计并产出修复清单 |
| [#62–#66](https://github.com/xiaxinyu/finsight/issues/62) | Merchant token / orphan / invalid / field drift / impact preview |
| [#69](https://github.com/xiaxinyu/finsight/issues/69) | 补齐二级分类 |
| [#70](https://github.com/xiaxinyu/finsight/issues/70) | 八步工作流产品化 |
| [#71–#73](https://github.com/xiaxinyu/finsight/issues/71) | Categories 资产页 / 规则质量 / dry-run |
| [#75](https://github.com/xiaxinyu/finsight/issues/75) | migration batch 审计表 |
| [#76–#77](https://github.com/xiaxinyu/finsight/issues/76) | 报表 Data Quality（v1.8.1） |

完整计划：[v1.8 数据基础版本计划](../roadmap/v1.8-data-foundation-version-plan.zh-cn.md)
