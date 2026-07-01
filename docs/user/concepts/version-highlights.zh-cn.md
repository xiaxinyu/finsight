# 版本功能要点（v2.0.0 → 当前）

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](version-highlights.md) |

> 完整清单：[function-list.zh-cn.md](../../tech/reference/function-list.zh-cn.md)

按 release 归纳**用户可感知**变更，便于对照界面与文档。

---

## v2.0.3 — 年度趋势与报表导航（当前主线）

**主题：** 收入 / 消费 / 负债 YoY 分析闭环 + Reports 信息架构。

| 域 | 变更摘要 |
| :--- | :--- |
| **YoY 报表** | Income Trends · Consumption Trends（改版）· Debt Trends |
| **API** | `/analytics/income-trends` · `/analytics/debt-trends` · 增强 `/analytics/trends` |
| **导航** | 6 组：Monthly overview · YoY trends · Spending analysis · Capital & taxes · Forecast & risk · Merchants |
| **命名** | Period Comparison · Transfers & Investments · Top Merchants / Merchant Changes |
| **Drill** | 修复趋势矩阵 semantic 下钻 0 行；面包屑与菜单一致 |
| **文档** | [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) · [v2.0.3 发布说明](../../tech/ops/v2.0.3-release-notes.zh-cn.md) |

---

## v2.0.2 — 专业财务语义

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
