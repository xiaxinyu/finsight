# Reports catalog

| | |
| :--- | :--- |
| **Language** | English · [简体中文](reports-catalog.zh-cn.md) |

> Semantics: [data-semantics.md](data-semantics.md) · UI: `/app/reports/*` · KPI **?** = `REPORT_METRIC_HINTS`

Reports are grouped by **decision question**, not by duplicate aggregations of the same totals.

---

## 1. Navigation map

```
Reports
├── Cashflow & budget     … cash position and plan vs actual
├── Spending              … structure and period change
├── Cash & outlook        … bills and forward view
└── Merchants             … payees, subscriptions, concentration
```

---

## 2. Cashflow & budget

### Cashflow

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Monthly Real income, Consumption, and net trend |
| **Data** | Semantic monthly metrics / period-summary |
| **Use when** | Month-end review; validate Dashboard bar chart |
| **Drill** | Category and transaction drill-down |

### Budget vs Actual

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | **Limit vs spent** per budget bucket |
| **Actual scope** | Consumption (`include_in_expense_trend`) |
| **Columns** | Reporting Classification · Limit · Spent · Utilization |
| **Use when** | Start/end of month vs Planning budgets |

### Fund Flow

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Internal transfer pairs excluded from spending |
| **Use when** | Reconcile card-to-card moves; avoid double-counting spend |

### Transfer & Finance

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Transfers, loans, investments — non-living flows |
| **Scope** | `non_pnl` semantic scope |
| **Use when** | Bank “outflows” ≠ Dashboard Consumption |

### Tax Summary

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Tax paid and refunds |
| **Scope** | `tax` |
| **Use when** | Annual tax review; separate from daily spend |

---

## 3. Spending

### Fixed vs Variable

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Fixed vs discretionary structure (`budget_behavior`) |
| **Use when** | Assess rigid cost load and cut room |

### Spending Drift

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | **Two-period** semantic bucket comparison |
| **Strength** | Comparable after category tree migrations (semantic tags) |
| **Use when** | “Where did I spend more this period vs last?” |

### Trend Changes

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Reporting Classification growth; savings-rate inflection |
| **Columns** | Classification · YoY delta · Share change |
| **Use when** | Structural shifts, not one-off spikes |

---

## 4. Cash & outlook

### Bills Calendar

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Fixed bills due in the next 30 days |
| **Use when** | Liquidity planning; avoid missed payments |

### Annual Outlook

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Statistical forecast with scenario bands (`hybrid_projection`) |
| **Use when** | Year planning; major purchase what-if |

### Cash Risk

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Projected deficit months and liquidity stress days |
| **Use when** | With Annual Outlook — “will I run short?” |

---

## 5. Merchants

### Subscriptions

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Recurring / subscription merchants |
| **Detection** | Pattern + category tags |
| **Use when** | Trim recurring spend |

### Merchant Concentration

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | Share of consumption at top merchants |
| **Use when** | Diversification and negotiation leverage |

### Merchant Drift

| | |
| :--- | :--- |
| <span style="color:#2563eb">**Purpose**</span> | YoY spend change by merchant |
| **Use when** | Find “quietly more expensive” vendors |

---

## 6. Shared capabilities (v2.0.2+)

| Capability | Description |
| :--- | :--- |
| **Unified Drill Drawer** | Insight → Breakdown → Transactions |
| **Semantic drill** | From donut/report slice by Reporting Classification → merchants |
| **Data quality bar** | Shared strip on all report pages |
| **Metric hints** | **?** on KPIs explains scope |

---

## 7. Quick lookup

| You need to know… | Open first |
| :--- | :--- |
| Earn / spend / save this period | Dashboard |
| Monthly trend | Cashflow |
| Over budget? | Budget vs Actual |
| Transfers / loans / investments | Transfer & Finance |
| Tax | Tax Summary |
| Fixed vs variable mix | Fixed vs Variable |
| Spend change between periods | Spending Drift |
| Which categories grew | Trend Changes |
| Upcoming bills | Bills Calendar |
| Full-year forecast | Annual Outlook |
| Cash shortfall risk | Cash Risk |
| Subscription spend | Subscriptions |
| Long-term financial type | Profile |

---

## 8. Related docs

- [dashboard-profile.md](dashboard-profile.md) — Dashboard & Profile  
- [version-highlights.md](version-highlights.md) — v2.0.x changes  
- [personal-finance-reporting-guide.md](../../tech/finance/personal-finance-reporting-guide.md) — engineering reference
