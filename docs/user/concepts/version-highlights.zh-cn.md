# 版本功能要点（v2.0.0 → 当前）

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](version-highlights.md) |

> 完整清单：[function-list.zh-cn.md](../../tech/reference/function-list.zh-cn.md)

按 release 归纳**用户可感知**变更，便于对照界面与文档。

---

## v2.0.2 — 专业财务语义（当前主线）

**主题：** Dashboard、Profile、Reports 共用同一 semantic layer。

| 域 | 变更摘要 |
| :--- | :--- |
| **语义层** | `v_transaction_finance_semantics`；月度 KPI `REAL_INCOME` / `CONSUMPTION_EXPENSE` |
| **Profile** | 物化 snapshot；Refresh 重算；10 维度 + confidence |
| **Dashboard** | 语义 KPI；Reporting Classification 饼图；semantic drill |
| **分类** | Admin finance semantics；可编辑 report_role / semantic tag |
| **Transactions** | Reporting Classification 全量筛选 |
| **Reports** | Transfer & Finance、Tax、统一 quality bar、metric ? |
| **术语** | Transfer / Finance / Investment（替代 Non-P&L） |

---

## v2.0.1 — 质量优化

可索引 date range SQL · Forecast hybrid · Profile GET 只读。

---

## v2.0.0 — 稳定性

Metric gate · read-path 统一。

---

## v1.8 — 分类治理

规则 impact preview · data quality 层 · 决策型报表导航。

---

## 关联文档

| 文档 | 用途 |
| :--- | :--- |
| [data-semantics.zh-cn.md](data-semantics.zh-cn.md) | 读 KPI |
| [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) | 报表索引 |
