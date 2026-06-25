# 个人财务报表口径指南（v2.0.2）

本文档说明 FinSight 核心数字的含义，与 [finance-semantic-contract.zh-cn.md](./finance-semantic-contract.zh-cn.md) 一致。

## Dashboard

| KPI | 含义 | 不包含 |
| --- | --- | --- |
| Real income | 计入收入趋势的真实收入（如工资） | 退款、报销、投资赎回、借款 |
| Consumption | 生活与预算类消费支出 | 转账、退款、投资买入、信用卡还款 |
| Net cashflow | Real income − Consumption | — |

数据来源：`GET /api/v1/analytics/metrics/period-summary`（语义视图）；无数据时回退交易报表。

## Profile / Forecast

- Profile 与 Forecast 优先使用 `REAL_INCOME`、`CONSUMPTION_EXPENSE` 月度指标。
- Profile GET 只读物化快照；需点击 Refresh 重算。

## Transactions

- 语义标签与筛选基于 `v_transaction_finance_semantics`。
- 筛选只改变查看范围，不修改交易本身。

## 报表一致性

Dashboard、Profile、Forecast 的核心收入/支出口径应对齐语义层。若发现不一致，先检查 metric refresh 与分类 `report_role`。

**数据质量提示**：所有 `/reports/*` 页面在顶部共用一条精简 Data quality 栏（`ReportsDataQualityBar`），不在各报表内重复展示。
