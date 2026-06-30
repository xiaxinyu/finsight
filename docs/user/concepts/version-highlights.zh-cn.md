# 版本功能要点（v2.0.0 → 当前）

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](version-highlights.md) |

> 工程清单：[function-list.zh-cn.md](../../tech/reference/function-list.zh-cn.md) · 计划：[v2.0.2 计划](../../tech/roadmap/v2.0.2-professional-finance-quality-plan.zh-cn.md)

---

## v2.0.2 — 专业财务语义（当前）

<span style="color:#2563eb">**主题**</span>：统一口径 · 物化分析 · 可解释报表

| 域 | 功能 |
| :--- | :--- |
| **语义层** | `v_transaction_finance_semantics` · 月度 `REAL_INCOME` / `CONSUMPTION_EXPENSE` |
| **Profile** | `fin_profile_current` 物化 · GET 只读 · Refresh 重算 |
| **Dashboard** | 语义 KPI · 饼图 drill · Metric hints |
| **分类** | Admin semantics · report_role 可编辑 · Transactions Reporting Classification 筛选 |
| **报表** | Transfer & Finance · Tax · Budget vs Actual · Spending Drift · Trend Changes · 统一 quality bar |
| **Drill** | Unified Drawer · semantic tag drill · legacy txnTypes 冲突修复 |
| **术语** | Non-P&L → Transfer / Finance / Investment |

---

## v2.0.1 — 质量优化

可索引 date range · Forecast hybrid · Profile GET 只读 · CI 恢复

---

## v2.0.0 — 稳定性

Metric gate · read-path 稳定 · L2 seed

---

## v1.8 — 分类治理

规则影响预览 · Data quality 层 · 决策型报表导航

---

## 近期 commit 摘要

| 范围 | 变更 |
| :--- | :--- |
| `feat(semantics)` | Catalog · semantic picker · semanticTag 持久化 |
| `feat(analytics)` | Semantic breakdown · period-summary |
| `feat(drilldown)` | `drillParamsForSemanticTag` |
| `feat(reports)` | Layout · metric explanations |
| `fix(transactions)` | Auto-classify 写回 · drill 过滤修复 |

---

## 关联文档

| 文档 | 用途 |
| :--- | :--- |
| [data-semantics.zh-cn.md](data-semantics.zh-cn.md) | 理解数字 |
| [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) | 报表目录 |
| [finance-semantic-contract.zh-cn.md](../../tech/finance/finance-semantic-contract.zh-cn.md) | 技术合同 |
