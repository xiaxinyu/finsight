# Drill down from a report

| | |
| :--- | :--- |
| **Language** | English · [简体中文](drill-down-from-reports.zh-cn.md) |

Open the **Unified Drill Drawer** from a chart or table row to see merchants and transactions behind a KPI slice.

---

## Prerequisites

- Transactions imported and classified.
- Dashboard **Period** matches the report you review.
- FinSight v2.0.2+ (semantic drill); v2.0.3 fixes year-over-year trend matrix drill.

---

## Steps

1. Open a report, for example:
   - **Fixed vs Variable** or **Period Comparison** (any two periods)
   - **Consumption / Income / Debt Trends** (year-over-year matrix)

2. Set **Period** or **compare years** (trend reports); optional Card / Category filters.

3. Click a **chart slice**, **bar**, or **matrix row** (e.g. Dining, Transport).

4. In the drill drawer:
   - **Insight** — totals for the slice.
   - **Breakdown** — merchants or sub-buckets.
   - **Transactions** — underlying rows.

5. Click a breakdown row to narrow to that merchant.

6. To fix a wrong row, open it in **Transactions** and edit category.

---

## Verification

| Check | Expected |
| :--- | :--- |
| Drawer title | Shows report context + semantic label (e.g. Dining) |
| Transaction count | Greater than zero when the slice has amount |
| Amount total | Matches the slice (same Period and filters) |
| Semantic drill | Uses **Reporting Classification** (`semanticFilter`), not legacy expense-only filter |
| Trend matrix | v2.0.3+ no longer returns zero rows from `consumeName` + semantic conflict |

---

## Notes

- **Other** rollup slices may not be drillable (virtual bucket).
- Drill inherits report **Card** / **Category** filters when set.
- **Period Comparison**: click the bar for the period you want (current vs compare).
- **Consumption / Income Trends**: matrix rows drill by semantic tag; calendar-year dates as `YYYY-MM-DD`.

---

## Related

- [reports-catalog.md](../concepts/reports-catalog.md)  
- [semantic-scenarios.md](../concepts/semantic-scenarios.md)  
- [reconcile-kpi-numbers.md](reconcile-kpi-numbers.md)
