# 个人财务报表口径（技术）

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](personal-finance-reporting-guide.md) |

> **用户文档：** [data-semantics.zh-cn.md](../../user/concepts/data-semantics.zh-cn.md) · [reports-catalog.zh-cn.md](../../user/concepts/reports-catalog.zh-cn.md)  
> **语义合同：** [finance-semantic-contract.zh-cn.md](./finance-semantic-contract.zh-cn.md)

本文面向研发与验收：API、视图、指标代码与一致性规则。

---

## 1. 数据源

| 消费方 | 主数据源 | 回退 |
| :--- | :--- | :--- |
| Dashboard KPI | `GET /api/v1/analytics/metrics/period-summary` | 交易聚合 |
| Dashboard 饼图 | `GET /api/v1/analytics/semantic-breakdown?scope=expense` | — |
| Profile | `fin_profile_current` 物化 | `fin_metric_monthly` |
| Reports | semantic-breakdown / 各 report service | 见各 mapper |
| Transactions 筛选 | `v_transaction_finance_semantics` | — |

统一视图：`v_transaction_finance_semantics`（Flyway V32+，V49 tag-driven inclusion）。

---

## 2. Headline KPI 映射

| UI 标签 | MetricCode | Inclusion |
| :--- | :--- | :--- |
| Real income | `REAL_INCOME` | `include_in_income_trend = 1` |
| Consumption | `CONSUMPTION_EXPENSE` | `include_in_expense_trend = 1` |
| Net | `NET_CASHFLOW` | 上两者差（或物化） |

---

## 3. Semantic breakdown scope

| scope | SQL 谓词摘要 |
| :--- | :--- |
| `expense` | `include_in_expense_trend = 1` |
| `income` | `include_in_income_trend = 1` |
| `non_pnl` | transfer / investment / liability 等 |
| `tax` | `tax_expense` · `tax_refund` tags |
| `refund` | `refund_reimbursement` |

实现：`SemanticBreakdownRepository.java`

---

## 4. Drill-down

| 端点 | 说明 |
| :--- | :--- |
| `GET /api/v1/transactions/drill-breakdown` | category · merchant · sample txns |

**v2.0.2+ 规则：** 当 `semanticFilter` 存在时，`TransactionMapper.filterTxnTypesT` **跳过** legacy `txn_types` 过滤，避免与 semantic tag 冲突。

---

## 5. Profile

| 端点 | 行为 |
| :--- | :--- |
| `GET /api/v1/analytics/profile` | 只读 `fin_profile_current` |
| `POST /api/v1/analytics/profile/refresh` | 重算并物化 |

指标优先：`REAL_INCOME` / `CONSUMPTION_EXPENSE`；缺失回退 `INCOME_TOTAL` / `EXPENSE_TOTAL`。

Runbook：[profile-materialization-runbook.zh-cn.md](./profile-materialization-runbook.zh-cn.md)

---

## 6. 一致性验收

1. Dashboard period Net ≈ Cashflow 报表同 period 汇总  
2. Budget vs Actual `Spent` 使用 consumption scope  
3. Transfer & Finance 总额不应出现在 Consumption  
4. Profile Refresh 后 `asOf` 更新；stale 告警消失  
5. metric gate mismatch 时 UI 展示 warning，不 silent 重算

---

## 7. 前端 hint 常量

| 文件 | 用途 |
| :--- | :--- |
| `frontend/src/components/MetricExplanation.tsx` | `DASHBOARD_METRIC_HINTS` · `REPORT_METRIC_HINTS` |
| `frontend/src/utils/reportTaxonomy.ts` | `REPORT_METRICS_SOURCE` · scope 标签 |

---

## 8. 变更规则

1. 修改 inclusion → 同步更新 semantic contract、Flyway 视图、测试  
2. 不得默认将退款/赎回/借款计入 income trend  
3. 不得默认将还款/转账/投资买入计入 expense trend  
