# Dashboard & Profile reading guide

| | |
| :--- | :--- |
| **Language** | English · [简体中文](dashboard-profile.zh-cn.md) |

> Semantic foundation: [data-semantics.md](data-semantics.md)

---

## 1. Page roles at a glance

| Page | Time window | Update model | Question |
| :--- | :--- | :--- | :--- |
| <span style="color:#2563eb">**Dashboard**</span> | **Period picker** (top-right) | Live query | How am I doing **in this period**? |
| <span style="color:#2563eb">**Profile**</span> | Fixed **last 12 months** | Materialized snapshot; **Refresh** to recompute | What **financial persona** am I over the long run? |

<span style="color:#d97706">**Note**</span>: Do not compare Dashboard Net for Jan–Jun with Profile overall score without aligning time and scope.

---

## 2. Dashboard layout

```
Period Picker ──► all KPIs & charts
     │
     ├─ Real income / Consumption / Net        ← semantic period-summary
     ├─ Cash flow chart                        ← monthly Real income · Consumption · Net
     ├─ Expense breakdown (donut)              ← top Reporting Classifications
     ├─ Data quality strip                     ← unclassified count → trust
     ├─ Advisor cards (feature flag)           ← suggested actions
     └─ Account balance panel                  ← balances (not period spend)
```

### 2.1 Headline KPIs

| KPI | How to read | Next step |
| :--- | :--- | :--- |
| Real income | Earned in period (semantic) | Open cash flow chart for monthly spikes |
| Consumption | Living spend in period | Check donut top-3 concentration |
| Net | Surplus or deficit | If negative → Spending Drift · Budget vs Actual |

**?** tooltip = same definitions as backend hints.

### 2.2 Interactions

| Action | Result |
| :--- | :--- |
| Click a month in Cash flow | Drill into that month’s structure |
| Click donut slice (e.g. Social) | Drill by **Reporting Classification** → merchants → transactions |

Semantic drill filters by **semantic tag**; legacy category `txn_types` filters are skipped when a semantic filter is active.

---

## 3. Profile layout

```
Overall score (0–100) + Confidence + User type
     │
     ├─ Radar: 10 dimensions
     ├─ Weakest / Strongest highlights
     └─ Dimension click → Reason · Evidence · Suggested actions
```

### 3.1 Ten dimensions

| ID | Label | Measures |
| :--- | :--- | :--- |
| `income_stability` | Income stability | Income volatility over 12 months |
| `spending_control` | Spending control | Expense vs income balance |
| `savings_discipline` | Savings discipline | Savings rate vs target |
| `fixed_burden` | Fixed burden | Fixed costs as % of income |
| `liquidity_safety` | Liquidity safety | Emergency runway (months) |
| `debt_pressure` | Debt pressure | Debt service vs income |
| `lifestyle_inflation` | Lifestyle inflation | Expense growth trend |
| `spending_concentration` | Spending concentration | Top-category share |
| `seasonality_risk` | Seasonality risk | Month-to-month net volatility |
| `data_trust` | Data trust | Classification completeness |

### 3.2 User types (examples)

Disciplined saver · High fixed burden · Cashflow stressed · Volatile income · Lifestyle inflation · Debt pressure · Data quality risk · Balanced

### 3.3 Snapshot states

| State | Meaning | Action |
| :--- | :--- | :--- |
| Not ready | No snapshot yet | **Generate profile** |
| Stale | Ledger changed since snapshot | **Refresh** |
| Reconciliation mismatch | Monthly metrics ≠ transaction recompute | Fix classification; see runbook |

<span style="color:#64748b">**Reference**</span>: [profile-materialization-runbook.zh-cn.md](../../tech/finance/profile-materialization-runbook.zh-cn.md)

---

## 4. Recommended cadence

| Cadence | Dashboard | Profile | Reports |
| :--- | :--- | :--- | :--- |
| Daily (~5 min) | Net + donut top 3 | — | — |
| Monthly (~15 min) | Confirm period | — | Cashflow · Budget vs Actual · Spending Drift |
| Quarterly (~30 min) | — | Refresh; review weakest 3 | Trend Changes · Annual Outlook · Cash Risk |

---

## 5. Related docs

- [reports-catalog.md](reports-catalog.md) — report index  
- [data-semantics.md](data-semantics.md) — KPI definitions
