# FinSight 财务语义合同（v2.0.2）

本文档定义每笔交易在专业报表中的统一语义口径。报表、Profile、Forecast、Dashboard 应通过 `v_transaction_finance_semantics` 视图或其派生指标读取 inclusion/exclusion，而不是在各 service 中重复判断 `is_transfer` / `is_refund` / `report_role`。

## 核心维度

| 字段 | 取值 | 含义 |
| --- | --- | --- |
| `cash_direction` | `inflow` / `outflow` / `neutral` | 现金方向 |
| `economic_nature` | `income` / `expense` / `transfer` / `refund` / `investment` / `liability` / `asset_adjustment` / `other` | 财务本质 |
| `report_role` | 来自 `cls_category.report_role` | 报表归属 |
| `budget_behavior` | `fixed` / `variable` / `essential` / `unclassified` | 预算行为 |
| `quality_state` | `classified` / `inferred` / `unclassified` | 数据可信度 |

## Inclusion 标志

| 标志 | 规则摘要 |
| --- | --- |
| `include_in_income_trend` | 真实收入：`direction=income` 且非转账、非退款，且 `report_role=income` |
| `include_in_expense_trend` | 消费支出：支出方向，排除转账、退款、负债、投资、资产调整 |
| `include_in_budget` | 计入预算：固定/预算类支出，排除转账、退款、投资、负债 |
| `include_in_cashflow` | 计入现金流：非转账 |
| `include_in_profile` | 计入 Profile 集中度等：非转账且已分类 |

## 典型场景

| 场景 | `economic_nature` | 收入趋势 | 支出趋势 | 预算 |
| --- | --- | --- | --- | --- |
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

## 实现位置

- SQL 视图：`src/main/resources/db/migration/V32__transaction_finance_semantics.sql`
- 月度指标聚合：`FinanceSemanticMetricsRepository` → `fin_metric_monthly`
- Profile 物化：`fin_profile_current`（`V33__fin_profile_current.sql`）
- GET `/api/v1/analytics/profile` 只读物化结果；POST `/api/v1/analytics/profile/refresh` 显式重算
- Profile / Forecast 优先使用 `REAL_INCOME`、`CONSUMPTION_EXPENSE`（缺失时回退 `INCOME_TOTAL` / `EXPENSE_TOTAL`）

## 变更规则

1. 修改 inclusion 逻辑必须同步更新本合同、V32 视图与相关测试。
2. 不得将退款、投资赎回、借款流入默认计入收入趋势。
3. 不得将投资买入、信用卡还款、账户转账默认计入生活消费压力。
