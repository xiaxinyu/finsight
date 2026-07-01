# Dashboard & Profile guide

| | |
| :--- | :--- |
| **Language** | English · [简体中文](dashboard-profile.zh-cn.md) |

> KPI definitions: [data-semantics.md](data-semantics.md)

---

## 1. Two pages, two questions

| Page | Time window | Updates | Question |
| :--- | :--- | :--- | :--- |
| **Dashboard** | You pick the **Period** (top-right) | Live query each time | How am I doing **now**? |
| **Profile** | Fixed **last 12 months** | Saved **snapshot**; click **Refresh** to rebuild | What is my **long-term** financial pattern? |

**Note:** Do not compare Dashboard Net (e.g. Jan–Jun) with Profile overall score without matching dates and scope.

---

## 2. Dashboard map

| Area | Shows | Action |
| :--- | :--- | :--- |
| **KPI cards** | Real income · Consumption · Net | Start here every visit |
| **Cash flow chart** | Same three metrics by month | Click a month to drill down |
| **Expense donut** | Top Reporting Classifications | Click a slice → merchants → transactions |
| **Data quality strip** | Unclassified count | Fix categories if trust is low |
| **Advisor cards** | Suggested next steps (if enabled) | Optional |
| **Account balance** | Current balances | Not the same as period spend |

### Read the KPIs

| KPI | Read it as | If it looks wrong |
| :--- | :--- | :--- |
| Real income | Earned money in the Period | Check [data-semantics.md](data-semantics.md); exclude refunds |
| Consumption | Living spend in the Period | Open donut; compare with Budget vs Actual |
| Net | Surplus (+) or gap (−) | If negative, open Period Comparison or Budget vs Actual |

**Drill-down:** Clicking a donut slice (e.g. Social) filters by **semantic tag**, not the old category tree only.

---

## 3. Profile map

| Area | Shows |
| :--- | :--- |
| **Overall score (0–100)** | Weighted health score |
| **Confidence** | How reliable the score is (data + history length) |
| **User type** | Short label (e.g. *Disciplined saver*, *Cashflow stressed*) |
| **Radar chart** | Ten dimension scores |
| **Weakest / Strongest** | Where to improve or maintain |
| **Dimension detail** | Reason, evidence, links to reports |

### Ten dimensions (plain English)

| Dimension | Measures |
| :--- | :--- |
| Income stability | Is income steady month to month? |
| Spending control | Do expenses stay below income? |
| Savings discipline | Are you saving enough? |
| Fixed burden | How much income goes to fixed bills? |
| Liquidity safety | How many months of runway do you have? |
| Debt pressure | How heavy are loan payments? |
| Lifestyle inflation | Is spending growing faster than income? |
| Spending concentration | Is spend stuck in a few categories? |
| Seasonality risk | How volatile is month-to-month net? |
| Data trust | How complete is classification? |

### Snapshot status

| Status | Meaning | Action |
| :--- | :--- | :--- |
| Not ready | No snapshot yet | **Generate profile** |
| Stale | Data changed after snapshot | **Refresh** |
| Reconciliation mismatch | Stored metrics ≠ live recompute | Fix categories; see engineering runbook |

---

## 4. How often to open each page

| How often | Dashboard | Profile | Reports |
| :--- | :--- | :--- | :--- |
| Daily (5 min) | Net + top 3 donut slices | — | — |
| Monthly (15 min) | Confirm Period | — | Cashflow · Budget vs Actual · Period Comparison |
| Quarterly (30 min) | — | Refresh; review 3 weakest dimensions | Income / Consumption / Debt Trends · Annual Outlook · Cash Risk |

---

## 5. Related docs

- [reports-catalog.md](reports-catalog.md)  
- [data-semantics.md](data-semantics.md)
