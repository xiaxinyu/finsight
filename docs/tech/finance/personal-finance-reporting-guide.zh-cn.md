# 个人财务报表口径（技术参考）

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](personal-finance-reporting-guide.md) |

> **用户指南：** [data-semantics.zh-cn.md](../../user/concepts/data-semantics.zh-cn.md) · [reports-catalog.zh-cn.md](../../user/concepts/reports-catalog.zh-cn.md)  
> **语义合同：** [finance-semantic-contract.zh-cn.md](./finance-semantic-contract.zh-cn.md)

面向研发与 QA：API、database view、metric code、验收清单。

---

## 1. 数据源

| UI 区域 | API / 表 | 回退 |
| :--- | :--- | :--- |
| Dashboard KPI | `GET /api/v1/analytics/metrics/period-summary` | 交易聚合 |
| Dashboard 饼图 | `GET /api/v1/analytics/semantic-breakdown?scope=expense` | — |
| Profile | 物化表 `fin_profile_current` | `fin_metric_monthly` |
| Reports | 各 report service + semantic breakdown | 见 mapper |
| Transactions 筛选 | `v_transaction_finance_semantics` | — |

Canonical view：`v_transaction_finance_semantics`（Flyway V32+，V49 tag-driven inclusion）。

---

## 2. Headline KPI 映射

| UI 标签 | MetricCode | Inclusion |
| :--- | :--- | :--- |
| Real income | `REAL_INCOME` | `include_in_income_trend = 1` |
| Consumption | `CONSUMPTION_EXPENSE` | `include_in_expense_trend = 1` |
| Net | `NET_CASHFLOW` | 派生或物化 |

---

## 3. Semantic breakdown scope

| scope | 含义 |
| :--- | :--- |
| `expense` | 生活消费趋势 |
| `income` | 真实收入趋势 |
| `non_pnl` | 转账、贷款、投资 |
| `tax` | 税费 |
| `refund` | 报销/退款 |

实现：`SemanticBreakdownRepository.java`

---

## 4. Drill-down API

`GET /api/v1/transactions/drill-breakdown`

存在 `semanticFilter` 时跳过 legacy `txn_types`（`TransactionMapper.filterTxnTypesT`）。

---

## 5. Profile API

| 方法 | 路径 | 行为 |
| :--- | :--- | :--- |
| GET | `/api/v1/analytics/profile` | 只读物化 snapshot |
| POST | `/api/v1/analytics/profile/refresh` | 重算并写入 |

优先 `REAL_INCOME` / `CONSUMPTION_EXPENSE`；回退 `INCOME_TOTAL` / `EXPENSE_TOTAL`。

---

## 6. 验收清单

1. Dashboard Net ≈ 同 period Cashflow  
2. Budget vs Actual Spent 为 consumption scope  
3. Transfer & Finance ∉ Consumption  
4. Profile Refresh 更新 `asOf`  
5. Metric gate mismatch 显示 warning，GET 不 silent 重算

---

## 7. 前端常量

| 文件 | 内容 |
| :--- | :--- |
| `MetricExplanation.tsx` | `DASHBOARD_METRIC_HINTS` · `REPORT_METRIC_HINTS` |
| `reportTaxonomy.ts` | Scope 标签 · 筛选 catalog |

---

## 8. 变更策略

1. Inclusion 变更 → 同步 contract、Flyway view、测试  
2. 禁止默认将 refund/赎回/借款计入 income trend  
3. 禁止默认将 还款/转账/投资买入计入 expense trend
