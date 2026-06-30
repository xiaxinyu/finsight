# 语义场景速查

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](semantic-scenarios.md) |

一页查 **交易如何影响 KPI**。完整规则：[finance-semantic-contract.zh-cn.md](../../tech/finance/finance-semantic-contract.zh-cn.md)。

---

## 列说明

| 列 | 含义 |
| :--- | :--- |
| **Income** | 计入 **Real income** |
| **Consumption** | 计入 **Consumption** |
| **Budget** | 计入 budget **Spent** |
| **Report** | 建议打开的报表 |

---

## 场景表

| 场景 | Income | Consumption | Budget | Report |
| :--- | :---: | :---: | :---: | :--- |
| 工资 | 是 | 否 | 否 | Dashboard / Cashflow |
| 劳务/经营收入 | 是 | 否 | 否 | Dashboard / Cashflow |
| 外出就餐 | 否 | 是 | 是 | Budget vs Actual |
| 房租 | 否 | 是 | 是 | Fixed vs Variable |
| 日用采购 | 否 | 是 | 是 | Budget vs Actual |
| 卡间/储蓄划转 | 否 | 否 | 否 | Transfer & Finance |
| 信用卡还款 | 否 | 否 | 否 | Transfer & Finance |
| 买入基金/股票 | 否 | 否 | 否 | Transfer & Finance |
| 卖出基金/股票 | 否 | 否 | 否 | Transfer & Finance |
| 消费退款 | 否 | 否 | 否 | Transactions (Refund) |
| 报销到账 | 否 | 否 | 否 | Transactions (Refund) |
| 缴纳所得税 | 否 | 否 | 否 | Tax Summary |
| 退税 | 否 | 否 | 否 | Tax Summary |
| 银行手续费 | 否 | 常是 | 常是 | 视 category tag |
| 未分类 | 否 | 否 | 否 | Transactions 补分类 |

---

## 与预期不符时

1. 查该行 **Reporting Classification**。  
2. Admin 查 category semantics。  
3. [reconcile-kpi-numbers.zh-cn.md](../tasks/reconcile-kpi-numbers.zh-cn.md)。
