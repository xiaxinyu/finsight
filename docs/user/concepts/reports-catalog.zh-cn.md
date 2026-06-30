# 报表目录

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](reports-catalog.md) |

> 口径：[data-semantics.zh-cn.md](data-semantics.zh-cn.md) · 路径：`/app/reports/*` · KPI **?** 说明 scope

每张报表对应**一个决策问题**。date range 一致时，total 与 Dashboard 使用同一 semantic layer。

---

## 1. 菜单分组

| 分组 | 决策问题 |
| :--- | :--- |
| **Cashflow & budget** | 赚、花、是否超预算？ |
| **Spending** | 结构如何？相对上期变化？ |
| **Cash & outlook** | 未来账单与风险？ |
| **Merchants** | 资金流向哪些商户？ |

---

## 2. Cashflow & budget

| 报表 | 用途 | 何时打开 | Scope |
| :--- | :--- | :--- | :--- |
| **Cashflow** | 月度 Real income / Consumption / Net | 月末复盘 | Semantic metrics |
| **Budget vs Actual** | Limit vs Spent | Planning 设预算后 | Consumption |
| **Fund Flow** | 内部转账对 | 卡间划转核对 | Transfer |
| **Transfer & Finance** | 转账、贷款、投资 | 银行支出 ≠ Consumption | `non_pnl` |
| **Tax Summary** | 税费与退税 | 年度税务 | `tax` |

---

## 3. Spending

| 报表 | 用途 | 何时打开 |
| :--- | :--- | :--- |
| **Fixed vs Variable** | 固定 vs 可变占比 | 成本结构分析 |
| **Spending Drift** | 两期 semantic 桶对比 | 「多花在哪儿？」 |
| **Trend Changes** | 分类增减、储蓄率拐点 | 结构性变化 |

---

## 4. Cash & outlook

| 报表 | 用途 | 何时打开 |
| :--- | :--- | :--- |
| **Bills Calendar** | 未来 30 天账单 | 流动性安排 |
| **Annual Outlook** | 预测与情景带 | 年度规划 |
| **Cash Risk** | 可能赤字月份 | 流动性压力测试 |

---

## 5. Merchants

| 报表 | 用途 | 何时打开 |
| :--- | :--- | :--- |
| **Subscriptions** |  recurring 商户 | 削减订阅 |
| **Merchant Concentration** | Top 商户占比 | 集中度风险 |
| **Merchant Drift** | 商户 YoY 变化 | 成本上升识别 |

---

## 6. 共用能力（v2.0.2+）

| 能力 | 说明 |
| :--- | :--- |
| Unified Drill Drawer | Insight → Breakdown → Transactions |
| Semantic drill | 图表切片 → merchant → 明细 |
| 下钻任务指南 | [drill-down-from-reports.zh-cn.md](../tasks/drill-down-from-reports.zh-cn.md) |
| Data quality bar | 报表顶栏统一提示 |
| Metric hints | KPI 旁 ? |

---

## 7. 速查

| 问题 | 打开 |
| :--- | :--- |
| 本期赚花剩 | Dashboard |
| 月度趋势 | Cashflow |
| 是否超预算 | Budget vs Actual |
| 转账/贷款/投资 | Transfer & Finance |
| 税务 | Tax Summary |
| 固定 vs 可变 | Fixed vs Variable |
| 两期对比 | Spending Drift |
| 哪类在涨 | Trend Changes |
| 未来账单 | Bills Calendar |
| 全年预测 | Annual Outlook |
| 流动性风险 | Cash Risk |
| 订阅 | Subscriptions |
| 长期类型 | Profile |

---

## 8. 关联文档

- [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md)  
- [version-highlights.zh-cn.md](version-highlights.zh-cn.md)  
- [personal-finance-reporting-guide.zh-cn.md](../../tech/finance/personal-finance-reporting-guide.zh-cn.md)
