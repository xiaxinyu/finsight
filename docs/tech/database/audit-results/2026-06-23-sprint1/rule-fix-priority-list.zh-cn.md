# 规则修复优先级清单 — Sprint 1（Issue #68）

日期：2026-06-23  
阶段：`BEFORE`  
汇总来源：`GET /api/v1/maintenance/classification-audit-summary` 或 `baseline-summary.json`

## 优先级定义

| 级别 | 含义 | 典型问题 |
|------|------|----------|
| **P0** | 数据完整性 / 报表口径错误 | orphan、invalid pattern、字段 drift、merchant token 失配 |
| **P1** | 分类覆盖率 / 规则质量 | 重复 pattern、过宽关键词、未分类 / OTHER 体量 |
| **P2** |  hygiene / 技术债 | 无 category 的规则、指向已删 category 的交易 |

## 自动生成 remediation plan

登录后请求：

```http
GET /api/v1/maintenance/classification-audit-summary
```

响应 `remediationPlan[]` 字段：`priority`, `area`, `issue`, `recommendedAction`, `remediationRef`, `countHint`。

## P0 — 立即处理（手动 SQL，禁止自动执行）

| # | 问题 | 脚本 / 动作 | count（运行 export 后填写） | 状态 |
|---|------|-------------|----------------------------|------|
| 1 | Active orphaned rules | [orphan-rules-remediation.sql](../../orphan-rules-remediation.sql) | | ☐ |
| 2 | Active invalid (blank pattern) rules | [invalid-rules-remediation.sql](../../invalid-rules-remediation.sql) | | ☐ |
| 3 | Category field drift vs consume_code | [transaction-category-field-remediation.sql](../../transaction-category-field-remediation.sql) | | ☐ |
| 4 | Merchant profile token mismatch | [merchant-token-normalization.sql](../../merchant-token-normalization.sql) | | ☐ |

## P1 — Sprint 1 规则 / 分类设计

| # | 问题 | 动作 | 导出文件 | count | 状态 |
|---|------|------|----------|-------|------|
| 5 | Duplicate active rule patterns | Rule Engine 合并 / 停用冲突规则 | `baseline-duplicate-patterns.csv` | | ☐ |
| 6 | Overly broad keywords | 收窄 pattern 或调整 priority | audit §7 | | ☐ |
| 7 | Unclassified transactions | 按 Top100 新增规则 | `baseline-unclassified-top100.csv` | | ☐ |
| 8 | OTHER / catch-all volume | 按 [l2-category-candidates.zh-cn.md](./l2-category-candidates.zh-cn.md) 拆分 L2 + 规则 | `baseline-other-consumption-top100.csv` | | ☐ |

## P2 —  backlog

| # | 问题 | 动作 | 状态 |
|---|------|------|------|
| 9 | Rules without category | 指定 category 或 archive | ☐ |
| 10 | Txns with missing/deleted category codes | 重映射 consume_code 或文档化例外 | ☐ |

## 执行顺序建议

1. 运行 `./scripts/db/export-classification-audit-baseline.sh` 生成本地 baseline CSV/JSON。
2. 填写上表 `count` 列（来自 `baseline-summary.json` 或 API）。
3. **P0** remediation SQL：逐脚本审阅 → 备份 → 手动执行。
4. **P1** 在 Rule Engine / Categories UI 完成；大批量改交易走 migration batch（[#75](https://github.com/xiaxinyu/finsight/issues/75)）。
5. 治理完成后保存 `AFTER` baseline 并对比 §20 指标。

## Sign-off

- [ ] BEFORE baseline 已 export
- [ ] P0 脚本均已审阅（未放入 Flyway）
- [ ] P1 规则 / L2 设计已与 l2-category-candidates 对齐
- [ ] AFTER baseline 待 Sprint 1 末执行

关联 Issue：[#68](https://github.com/xiaxinyu/finsight/issues/68)
