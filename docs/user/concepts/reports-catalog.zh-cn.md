# 报表目录

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](reports-catalog.md) |

> 口径：[data-semantics.zh-cn.md](data-semantics.zh-cn.md) · 路径：`/app/reports/*` · KPI **?** 说明 scope  
> **v2.0.3：** 年度趋势三件套 + 六组导航 — [version-highlights.zh-cn.md](version-highlights.zh-cn.md)

每张报表对应**一个决策问题**。date range 一致时，total 与 Dashboard 使用同一 semantic layer。

---

## 1. 菜单分组

| 分组 | 决策问题 |
| :--- | :--- |
| **Monthly overview** | 本月赚、花、账期如何？ |
| **Year-over-year trends** | 收入、消费、负债按日历年如何变化？ |
| **Spending analysis** | 支出结构如何？任意两期对比？ |
| **Capital & taxes** | 转账、投资、贷款、税务（不计入日常消费） |
| **Forecast & risk** | 未来账单与流动性风险？ |
| **Merchants** | 资金流向哪些商户？ |

---

## 2. Monthly overview

| 报表 | 用途 | 何时打开 | Scope |
| :--- | :--- | :--- | :--- |
| **Cashflow** | 月度 Real income / Consumption / Net | 月末复盘 | Semantic metrics |
| **Budget vs Actual** | Limit vs Spent | Planning 设预算后 | Consumption |
| **Bills Calendar** | 未来 30 天账单 | 流动性安排 | Bills |

---

## 3. Year-over-year trends

| 报表 | 用途 | 何时打开 |
| :--- | :--- | :--- |
| **Income Trends** | 日历年收入 YoY + 分类矩阵 | 收入是否增长 |
| **Consumption Trends** | 日历年消费 YoY + 分类/L1 矩阵 · CSV | 消费历史分析 |
| **Debt Trends** | 借贷、还款、净负债流 YoY | 负债变化趋势 |

---

## 4. Spending analysis

| 报表 | 用途 | 何时打开 |
| :--- | :--- | :--- |
| **Fixed vs Variable** | 固定 vs 可变占比 | 成本结构分析 |
| **Period Comparison** | 任意两期按 Classification 对比 | 季度、半年等自定义区间 |

---

## 5. Capital & taxes

| 报表 | 用途 | 何时打开 | Scope |
| :--- | :--- | :--- | :--- |
| **Fund Flow** | 内部转账对 | 卡间划转核对 | Transfer |
| **Transfers & Investments** | 转账、贷款、投资 | 银行支出 ≠ Consumption | `non_pnl` |
| **Tax Summary** | 税费与退税 | 年度税务 | `tax` |

---

## 6. Forecast & risk

| 报表 | 用途 | 何时打开 |
| :--- | :--- | :--- |
| **Annual Outlook** | 预测与情景带 | 年度规划 |
| **Cash Risk** | 可能赤字月份 | 流动性压力测试 |

---

## 7. Merchants

| 报表 | 用途 | 何时打开 |
| :--- | :--- | :--- |
| **Subscriptions** | recurring 商户 | 削减订阅 |
| **Top Merchants** | Top 商户占比 | 集中度风险 |
| **Merchant Changes** | 商户 YoY 变化 | 成本上升识别 |

---

## 8. 共用能力（v2.0.2+）

| 能力 | 说明 |
| :--- | :--- |
| Unified Drill Drawer | Insight → Breakdown → Transactions |
| Semantic drill | 图表切片 → merchant → 明细 |
| 下钻任务指南 | [drill-down-from-reports.zh-cn.md](../tasks/drill-down-from-reports.zh-cn.md) |
| Data quality bar | 报表顶栏统一提示 |
| Metric hints | KPI 旁 ? |

---

## 9. 速查

| 问题 | 打开 |
| :--- | :--- |
| 本期赚花剩 | Dashboard |
| 月度趋势 | Cashflow |
| 是否超预算 | Budget vs Actual |
| 转账/贷款/投资 | Transfers & Investments |
| 税务 | Tax Summary |
| 固定 vs 可变 | Fixed vs Variable |
| 自定义两期对比 | Period Comparison |
| 逐年收入 | Income Trends |
| 逐年消费 | Consumption Trends |
| 逐年负债 | Debt Trends |
| 未来账单 | Bills Calendar |
| 全年预测 | Annual Outlook |
| 流动性风险 | Cash Risk |
| 订阅 | Subscriptions |
| 长期类型 | Profile |

---

## 10. 关联文档

- [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md)  
- [version-highlights.zh-cn.md](version-highlights.zh-cn.md)  
- [personal-finance-reporting-guide.zh-cn.md](../../tech/finance/personal-finance-reporting-guide.zh-cn.md)
