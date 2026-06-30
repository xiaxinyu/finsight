# Reports catalog

| | |
| :--- | :--- |
| **Language** | English · [简体中文](reports-catalog.zh-cn.md) |

> KPI rules: [data-semantics.md](data-semantics.md) · UI path: `/app/reports/*` · Hover **?** on KPIs for scope text

Each report answers **one decision question**. Totals use the same semantic layer as the Dashboard when the date range matches.

---

## 1. Menu structure

| Group | Question it helps answer |
| :--- | :--- |
| **Cashflow & budget** | Did I earn, spend, and stay within plan? |
| **Spending** | How is my spend structured and changing? |
| **Cash & outlook** | What bills and risks are ahead? |
| **Merchants** | Who receives my money? |

---

## 2. Cashflow & budget

| Report | Purpose | When to open | Scope |
| :--- | :--- | :--- | :--- |
| **Cashflow** | Monthly Real income, Consumption, Net | Month-end review | Semantic metrics |
| **Budget vs Actual** | Budget limit vs actual spend per bucket | After setting budgets in Planning | Consumption |
| **Fund Flow** | Internal transfer pairs | Reconcile card-to-card moves | Transfers |
| **Transfer & Finance** | Transfers, loans, investments | Bank outflow ≠ Consumption | `non_pnl` |
| **Tax Summary** | Tax paid and refunds | Annual tax review | `tax` |

---

## 3. Spending

| Report | Purpose | When to open |
| :--- | :--- | :--- |
| **Fixed vs Variable** | Share of fixed vs flexible spend | Cut costs; understand rigid bills |
| **Spending Drift** | Compare two periods by semantic bucket | “Where did I spend more?” |
| **Trend Changes** | Category growth and savings-rate shifts | Structural change, not one-off spikes |

---

## 4. Cash & outlook

| Report | Purpose | When to open |
| :--- | :--- | :--- |
| **Bills Calendar** | Bills due in next 30 days | Cash planning |
| **Annual Outlook** | Forecast with scenario bands | Year plan; large purchases |
| **Cash Risk** | Months that may show negative net | Liquidity stress test |

---

## 5. Merchants

| Report | Purpose | When to open |
| :--- | :--- | :--- |
| **Subscriptions** | Recurring merchants | Reduce recurring fees |
| **Merchant Concentration** | Spend share at top merchants | Dependency risk |
| **Merchant Drift** | Year-over-year change by merchant | Find rising vendors |

---

## 6. Shared UI (v2.0.2+)

| Feature | What it does |
| :--- | :--- |
| **Unified Drill Drawer** | Insight → Breakdown → Transactions |
| **Semantic drill** | From chart slice to merchants to rows |
| **Drill task guide** | [drill-down-from-reports.md](../tasks/drill-down-from-reports.md) |
| **Data quality bar** | Same warning strip on all report pages |
| **Metric hints** | **?** explains inclusion rules |

---

## 7. Quick lookup

| I need to know… | Open |
| :--- | :--- |
| Earn / spend / save this period | Dashboard |
| Trend by month | Cashflow |
| Over budget? | Budget vs Actual |
| Transfers / loans / investments | Transfer & Finance |
| Tax | Tax Summary |
| Fixed vs flexible mix | Fixed vs Variable |
| Spend vs last period | Spending Drift |
| Which categories grew | Trend Changes |
| Upcoming bills | Bills Calendar |
| Full-year forecast | Annual Outlook |
| Risk of running short | Cash Risk |
| Subscription total | Subscriptions |
| Long-term financial type | Profile |

---

## 8. Related docs

- [dashboard-profile.md](dashboard-profile.md)  
- [version-highlights.md](version-highlights.md)  
- [personal-finance-reporting-guide.md](../../tech/finance/personal-finance-reporting-guide.md) (developers)
