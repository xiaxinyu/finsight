# 数据语义：如何理解 FinSight 的数字

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](data-semantics.md) |

> 阅读标记：[_style-guide.zh-cn.md](_style-guide.zh-cn.md)  
> 技术实现：[finance-semantic-contract.zh-cn.md](../../tech/finance/finance-semantic-contract.zh-cn.md)

FinSight 不是「银行流水求和」，而是先给每笔交易打上**财务语义**，再决定它进入哪个 KPI。

---

## 1. 两层分类

| 层级 | 界面名称 | 回答的问题 | 示例 |
| :--- | :--- | :--- | :--- |
| <span style="color:#2563eb">**Transaction type**</span> | 交易类型 | 现金方向上的大类 | Income · Expense · Transfer · Finance · Tax · Refund |
| <span style="color:#2563eb">**Reporting Classification**</span> | 报表分类 | 分析里归哪一桶 | Dining · Social · Housing · Loan · Investment |

<span style="color:#2563eb">**核心原则**</span>：Dashboard、Profile、报表的聚合以 **Reporting Classification + inclusion 标志** 为准，而非银行「支出」标签。

---

## 2. 三个 headline KPI

| KPI | <span style="color:#059669">计入</span> | <span style="color:#d97706">不计入</span> |
| :--- | :--- | :--- |
| **Real income** 真实收入 | 工资、经营收入等 `report_role=income` | 退款、报销、投资赎回、借款流入 |
| **Consumption** 生活消费 | 日常与预算类支出 | 转账、信用卡还款、投资买入、退款 |
| **Net cashflow** 净现金流 | Real income − Consumption | — |

**数据源**：`v_transaction_finance_semantics` → `REAL_INCOME` / `CONSUMPTION_EXPENSE`。Dashboard KPI 旁 **?** 与后端 `DASHBOARD_METRIC_HINTS` 一致。

---

## 3. 非生活性资金流

| 类型 | 入口 | 为何单独列 |
| :--- | :--- | :--- |
| 账户转账 | Fund Flow · Transfer & Finance | 不影响消费压力 |
| 贷款 / 还款 | Transfer & Finance | 负债，非生活支出 |
| 投资买卖 | Transfer & Finance | 资产配置 |
| 税费 | Tax Summary | 与日常消费决策分离 |
| 退款 / 报销 | Transactions → Refund | 不进收入趋势 |

<span style="color:#d97706">**注意**</span>：银行 App「本月支出」通常 **大于** Dashboard Consumption — 口径差异，非 bug。

---

## 4. 数据质量

| 状态 | 含义 | 建议 |
| :--- | :--- | :--- |
| `classified` | 分类明确 | 可用于报表 |
| `inferred` | 规则推断 | 抽查大额 |
| `unclassified` | 未分类 | 先补分类再看 Profile |

未分类越多 → Dashboard / Profile **Data trust** 越低。

---

## 5. 数字对不齐 — 排查顺序

1. **Period** — Dashboard 自选区间；Profile 固定 12 个月  
2. **Scope** — 消费 vs 转账 vs 税务报表  
3. **分类** — Admin 改 semantic tag 后 Refresh Profile  
4. **Stale** — Profile Refresh；reconciliation 见 [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md)

---

## 6. 心智模型

```
原始交易
  → Transaction type（方向桶）
  → Reporting Classification（semantic_tag）
  → Inclusion 标志（收入趋势？支出趋势？预算？）
  → KPI / 报表 / Profile 维度
```

---

## 7. 关联文档

| 文档 | 用途 |
| :--- | :--- |
| [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md) | Dashboard 与 Profile |
| [reports-catalog.zh-cn.md](reports-catalog.zh-cn.md) | 报表目录 |
| [version-highlights.zh-cn.md](version-highlights.zh-cn.md) | 版本功能 |
| [personal-finance-reporting-guide.zh-cn.md](../../tech/finance/personal-finance-reporting-guide.zh-cn.md) | 技术验收 |
