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
| **Monthly overview** | How did I earn, spend, and what bills are due? |
| **Year-over-year trends** | How are income, consumption, and debt changing by calendar year? |
| **Spending analysis** | How is spend structured? How do two periods compare? |
| **Capital & taxes** | Transfers, investments, loans, tax — outside daily spending |
| **Forecast & risk** | What bills and liquidity risks are ahead? |
| **Merchants** | Who receives my money? |

---

## 2. Monthly overview

| Report | Purpose | When to open | Scope |
| :--- | :--- | :--- | :--- |
| **Cashflow** | Monthly Real income, Consumption, Net | Month-end review | Semantic metrics |
| **Budget vs Actual** | Budget limit vs actual spend per bucket | After setting budgets in Planning | Consumption |
| **Bills Calendar** | Bills due in next 30 days | Cash planning | Bills |

---

## 3. Year-over-year trends

| Report | Purpose | When to open |
| :--- | :--- | :--- |
| **Income Trends** | Calendar-year income YoY + source matrix | Is income growing? |
| **Consumption Trends** | Calendar-year spend YoY + matrix · CSV export | Historical spend analysis |
| **Debt Trends** | Borrowing, repayments, net debt flow YoY | Liability trend |

---

## 4. Spending analysis

| Report | Purpose | When to open |
| :--- | :--- | :--- |
| **Fixed vs Variable** | Share of fixed vs flexible spend | Cut costs; understand rigid bills |
| **Period Comparison** | Compare two periods by classification | Custom range (quarter, half-year) |

---

## 5. Capital & taxes

| Report | Purpose | When to open | Scope |
| :--- | :--- | :--- | :--- |
| **Fund Flow** | Internal transfer pairs | Reconcile card-to-card moves | Transfers |
| **Transfers & Investments** | Transfers, loans, investments | Bank outflow ≠ Consumption | `non_pnl` |
| **Tax Summary** | Tax paid and refunds | Annual tax review | `tax` |

---

## 6. Forecast & risk

| Report | Purpose | When to open |
| :--- | :--- | :--- |
| **Annual Outlook** | Forecast with scenario bands | Year plan; large purchases |
| **Cash Risk** | Months that may show negative net | Liquidity stress test |

---

## 7. Merchants

| Report | Purpose | When to open |
| :--- | :--- | :--- |
| **Subscriptions** | Recurring merchants | Reduce recurring fees |
| **Top Merchants** | Spend share at top merchants | Dependency risk |
| **Merchant Changes** | Year-over-year change by merchant | Find rising vendors |

---

## 8. Shared UI (v2.0.2+)

| Feature | What it does |
| :--- | :--- |
| **Unified Drill Drawer** | Insight → Breakdown → Transactions |
| **Semantic drill** | From chart slice to merchants to rows |
| **Drill task guide** | [drill-down-from-reports.md](../tasks/drill-down-from-reports.md) |
| **Data quality bar** | Same warning strip on all report pages |
| **Metric hints** | **?** explains inclusion rules |

---

## 9. Quick lookup

| I need to know… | Open |
| :--- | :--- |
| Earn / spend / save this period | Dashboard |
| Trend by month | Cashflow |
| Over budget? | Budget vs Actual |
| Transfers / loans / investments | Transfers & Investments |
| Tax | Tax Summary |
| Fixed vs flexible mix | Fixed vs Variable |
| Spend vs custom period | Period Comparison |
| Year-over-year income | Income Trends |
| Year-over-year consumption | Consumption Trends |
| Year-over-year debt | Debt Trends |
| Upcoming bills | Bills Calendar |
| Full-year forecast | Annual Outlook |
| Risk of running short | Cash Risk |
| Subscription total | Subscriptions |
| Long-term financial type | Profile |

---

## 10. Related docs

- [dashboard-profile.md](dashboard-profile.md)  
- [version-highlights.md](version-highlights.md)  
- [personal-finance-reporting-guide.md](../../tech/finance/personal-finance-reporting-guide.md) (developers)
