# 报表目录与功能说明

| | |
| :--- | :--- |
| **Language** | 简体中文 · [English](reports-catalog.md) |

> 口径：[data-semantics.zh-cn.md](data-semantics.zh-cn.md) · UI：`/app/reports/*` · KPI **?** = `REPORT_METRIC_HINTS`

按**决策问题**分组，避免重复堆叠同一总数。

---

## 1. 导航结构

```
Reports
├── Cashflow & budget
├── Spending
├── Cash & outlook
└── Merchants
```

---

## 2. Cashflow & budget

| 报表 | <span style="color:#2563eb">目的</span> | 数据源 / Scope | 何时用 |
| :--- | :--- | :--- | :--- |
| **Cashflow** | 月度 Real income · Consumption · Net | period-summary | 月度复盘 |
| **Budget vs Actual** | Limit vs Spent | Consumption scope | 对照 Planning |
| **Fund Flow** | 内部转账对 | 转账配对 | 避免重复计支出 |
| **Transfer & Finance** | 转账/贷款/投资 | `non_pnl` | 银行支出 ≠ Consumption |
| **Tax Summary** | 税费与退税 | `tax` | 年度税务复盘 |

---

## 3. Spending

| 报表 | <span style="color:#2563eb">目的</span> | 要点 |
| :--- | :--- | :--- |
| **Fixed vs Variable** | 固定 vs 可变结构 | `budget_behavior` |
| **Spending Drift** | 两期语义桶对比 | 分类迁移后仍可比 |
| **Trend Changes** | Classification YoY · 储蓄拐点 | 结构性变化 |

---

## 4. Cash & outlook

| 报表 | <span style="color:#2563eb">目的</span> |
| :--- | :--- |
| **Bills Calendar** | 未来 30 天固定账单 |
| **Annual Outlook** | hybrid_projection 情景预测 |
| **Cash Risk** | 赤字月份 · 流动性压力日 |

---

## 5. Merchants

| 报表 | <span style="color:#2563eb">目的</span> |
| :--- | :--- |
| **Subscriptions** |  recurring / 订阅商户 |
| **Merchant Concentration** | 消费集中度 |
| **Merchant Drift** | 商户 YoY 变化 |

---

## 6. 共用能力（v2.0.2+）

| 能力 | 说明 |
| :--- | :--- |
| Unified Drill Drawer | Insight → Breakdown → Transactions |
| Semantic drill | 按 Reporting Classification 下钻商户 |
| Data quality bar | Reports 顶栏统一提示 |
| Metric hints | KPI 旁 ? |

---

## 7. 速查表

| 问题 | 打开 |
| :--- | :--- |
| 本期赚花剩 | Dashboard |
| 月度趋势 | Cashflow |
| 预算超支 | Budget vs Actual |
| 转账/投资/贷款 | Transfer & Finance |
| 税 | Tax Summary |
| 两期变化 | Spending Drift |
| 哪类在涨 | Trend Changes |
| 未来账单 | Bills Calendar |
| 全年预测 | Annual Outlook |
| 会不会缺钱 | Cash Risk |
| 订阅 | Subscriptions |
| 长期类型 | Profile |

---

## 8. 关联文档

- [dashboard-profile.zh-cn.md](dashboard-profile.zh-cn.md)  
- [version-highlights.zh-cn.md](version-highlights.zh-cn.md)  
- [personal-finance-reporting-guide.zh-cn.md](../../tech/finance/personal-finance-reporting-guide.zh-cn.md)
