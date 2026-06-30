# Drill down from a report

| | |
| :--- | :--- |
| **Language** | English · [简体中文](drill-down-from-reports.zh-cn.md) |

Open the **Unified Drill Drawer** from a chart or table row to see merchants and transactions behind a KPI slice.

---

## Prerequisites

- Transactions imported and classified.
- Dashboard **Period** matches the report you review.
- FinSight v2.0.2+ (semantic drill).

---

## Steps

1. Open a report, e.g. **Fixed vs Variable** or **Spending Drift** (`/app/reports/*`).

2. Set **Period** (and optional Card / Category filters).

3. Click a **chart slice** or **table row** (e.g. Dining, Housing).

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
| Semantic drill | Uses **Reporting Classification**, not legacy expense-only filter |

---

## Notes

- **Other** rollup slices may not be drillable (virtual bucket).
- Drill inherits report **Card** / **Category** filters when set.
- Spending Drift: click the bar for the period you want (current vs compare).

---

## Related

- [reports-catalog.md](../concepts/reports-catalog.md)  
- [semantic-scenarios.md](../concepts/semantic-scenarios.md)  
- [reconcile-kpi-numbers.md](reconcile-kpi-numbers.md)
