# FinSight 财务语义合同（v2.0.2）

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](finance-semantic-contract.md) |

> 用户指南：[data-semantics.zh-cn.md](../../user/concepts/data-semantics.zh-cn.md)

本文定义每笔交易在 Dashboard、Profile、Forecast、Reports 中的**统一语义口径**。

**代码规则：** 通过视图 `v_transaction_finance_semantics`（或其物化指标）读取 inclusion/exclusion；禁止在各 service 重复散落判断 `is_transfer` / `is_refund` / `report_role`。

---

## 1. 核心维度

| 字段 | 取值 | 含义 |
| :--- | :--- | :--- |
| `cash_direction` | `inflow` / `outflow` / `neutral` | 现金方向 |
| `economic_nature` | `income` / `expense` / `transfer` / `refund` / `investment` / `liability` / `asset_adjustment` / `other` | 财务本质 |
| `report_role` | 来自 `cls_category.report_role` | 报表归属 |
| `budget_behavior` | `fixed` / `variable` / `essential` / `unclassified` | 预算行为 |
| `quality_state` | `classified` / `inferred` / `unclassified` | 数据可信度 |

---

## 2. Inclusion 标志

| 标志 | 规则摘要 |
| :--- | :--- |
| `include_in_income_trend` | 真实收入：inflow + `report_role=income`；非 transfer/refund |
| `include_in_expense_trend` | 生活消费：expense outflow；排除 transfer、refund、liability、investment、asset_adjustment |
| `include_in_budget` | 预算桶：固定/预算类支出；排除 transfer、refund、investment、liability |
| `include_in_cashflow` | 现金流视图：非 transfer |
| `include_in_profile` | Profile 集中度：已分类且非 transfer |

---

## 3. 典型场景

| 场景 | `economic_nature` | 收入趋势 | 支出趋势 | 预算 |
| :--- | :--- | :---: | :---: | :---: |
| 工资 | `income` | 是 | 否 | 否 |
| 报销到账 | `refund` | 否 | 否 | 否 |
| 消费退款 | `refund` | 否 | 否 | 否 |
| 信用卡还款 | `liability` | 否 | 否 | 否 |
| 基金申购 | `investment` | 否 | 否 | 否 |
| 基金赎回 | `investment` | 否 | 否 | 否 |
| 账户间转账 | `transfer` | 否 | 否 | 否 |
| 手续费/利息 | `expense` | 否 | 视分类 | 视分类 |
| 借款收到 | `liability` | 否 | 否 | 否 |
| 未分类 | `other` | 否 | 否 | 否 |

---

## 4. 实现位置

| 层级 | 路径 |
| :--- | :--- |
| SQL 视图 | `V32__transaction_finance_semantics.sql`（至 V49 tag-driven inclusion） |
| 月度指标 | `FinanceSemanticMetricsRepository` → `fin_metric_monthly` |
| Profile 物化 | `fin_profile_current` |
| Profile GET | `/api/v1/analytics/profile` — 只读 |
| Profile Refresh | `POST /api/v1/analytics/profile/refresh` |
| 优先 metric | `REAL_INCOME`、`CONSUMPTION_EXPENSE`（回退 `INCOME_TOTAL` / `EXPENSE_TOTAL`） |

---

## 5. 变更规则

1. 修改 inclusion → 同步本合同、Flyway 视图、测试。  
2. 不得将退款、投资赎回、借款流入默认计入 income trend。  
3. 不得将投资买入、信用卡还款、转账默认计入 expense trend。

---

## 6. 关联文档

| 文档 | 用途 |
| :--- | :--- |
| [data-semantics.zh-cn.md](../../user/concepts/data-semantics.zh-cn.md) | KPI 定义 |
| [semantic-scenarios.zh-cn.md](../../user/concepts/semantic-scenarios.zh-cn.md) | 场景速查 |
| [personal-finance-reporting-guide.zh-cn.md](./personal-finance-reporting-guide.zh-cn.md) | 研发 / QA |
| [refresh-profile.zh-cn.md](../../user/tasks/refresh-profile.zh-cn.md) | 刷新 Profile |
