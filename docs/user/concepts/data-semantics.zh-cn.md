# 数据语义 — 如何理解 FinSight 指标

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](data-semantics.md) |

> 规范：[_style-guide.zh-cn.md](_style-guide.zh-cn.md) · 实现：[finance-semantic-contract.zh-cn.md](../../tech/finance/finance-semantic-contract.zh-cn.md)

---

## 为何与银行 App 数字不同

银行展示的是**全部流出**。FinSight 将**生活消费**与**转账、负债、投资、税费**分开统计。

系统不对 raw statement 简单求和。每笔交易先赋予 **finance semantic（财务语义）**，再由 **inclusion 规则**决定进入哪些 **KPI（关键绩效指标）**。

---

## 1. 双层标签体系

| 层级 | 界面字段 | 解答的问题 | 示例 |
| :--- | :--- | :--- | :--- |
| **核心 — Transaction type** | 交易类型 | 现金方向桶 | Income · Expense · Transfer · Finance · Tax · Refund |
| **核心 — Reporting Classification** | 报表分类 | 分析桶 | Dining · Social · Housing · Loan · Investment |

**核心规则：** Dashboard、Profile、Reports 按 **Reporting Classification + inclusion 标志** 聚合，而非仅依银行借贷方向。

---

## 2. 三个 headline KPI

| KPI | 业务含义 | **计入** | **不计入** |
| :--- | :--- | :--- | :--- |
| **Real income** | 真实收入 | 工资、经营所得 | 退款、报销、投资赎回、借款流入 |
| **Consumption** | 生活消费 | 日常与 budget scope 内支出 | 转账、信用卡还款、投资买入、退款 |
| **Net cashflow** | 净现金流 | Real income − Consumption | — |

**数据链路：** `v_transaction_finance_semantics` → `REAL_INCOME` / `CONSUMPTION_EXPENSE`。

**界面：** Dashboard KPI 旁 **?** 与上述定义一致。

---

## 3. 需单独查看的资金流

| 类型 | 入口 | 原因 |
| :--- | :--- | :--- |
| 账户间划转 | Fund Flow · Transfer & Finance | 非 consumption pressure |
| 贷款/还款 | Transfer & Finance | liability，非生活支出 |
| 投资买卖 | Transfer & Finance | asset allocation |
| 税费 | Tax Summary | 与日常 budget 分离 |
| 退款/报销 | Transactions → Refund | 不计入 income trend |

**注意：** 银行「本月支出」通常高于 Dashboard **Consumption** — scope 差异，非系统缺陷。

---

## 4. 数据质量（data quality）

| 状态 | 含义 | 建议动作 |
| :--- | :--- | :--- |
| Classified | 分类已确认 | 可用于报表 |
| Inferred | 规则推断 | 复核大额 |
| Unclassified | 未分类 | 先归类再 Refresh Profile |

未分类越多 → Dashboard / Profile **Data trust** 越低。

---

## 5. 数字不一致 — 排查清单

1. **Date range 是否一致？** Dashboard 用 Period；Profile 固定近 12 个月。  
2. **Report scope 是否一致？** 消费 / 转账 / 税务报表不可直接对比 total。  
3. **分类是否变更？** Admin 修改 semantic tag 后需 **Profile → Refresh**。  
4. **Snapshot 是否 stale？** 见 [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md)。

---

## 6. 处理流水线

```
银行流水
  → Transaction type
  → Reporting Classification（semantic tag）
  → Inclusion 标志
  → KPI / Report / Profile 维度
```

---

## 7. 关联文档

| 文档 | 用途 |
| :--- | :--- |
| [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md) | Dashboard 与 Profile |
| [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) | 报表目录 |
| [version-highlights.zh-cn.md](version-highlights.zh-cn.md) | 版本功能 |
| [semantic-scenarios.zh-cn.md](semantic-scenarios.zh-cn.md) | 场景速查 |
| [personal-finance-reporting-guide.zh-cn.md](../../tech/finance/personal-finance-reporting-guide.zh-cn.md) | API（研发） |
