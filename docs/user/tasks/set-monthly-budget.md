# Set a monthly budget

| | |
| :--- | :--- |
| **Language** | English · [简体中文](set-monthly-budget.zh-cn.md) |

Set a **monthly spending limit** and compare it to **Consumption** in Budget vs Actual.

---

## Prerequisites

- Logged in.
- Transactions imported.
- You know your target monthly living spend.

---

## Steps

1. Open **Planning** (`/app/planning`).

2. Tab **Overview** → section **Monthly budget**.

3. Enter **limit amount** (CNY).

4. Click **Save budget** (or equivalent save action).

5. Open **Reports → Budget vs Actual** (`/app/reports/budget-vs-actual`).

6. Match the report **Period** to the month you review.

7. Compare **Limit** vs **Spent** (Spent uses **Consumption** scope).

---

## Verification

| Check | Expected |
| :--- | :--- |
| Planning save | Success message |
| Budget vs Actual | Limit matches your saved amount |
| Utilization | Spent ÷ Limit reflects real progress |

---

## Notes

- Current UI saves one bucket: `all` (total monthly cap).
- **Spent** excludes transfers, loan repayments, and investments — same as Dashboard **Consumption**.

---

## Related

- [data-semantics.md](../concepts/data-semantics.md)  
- [reports-catalog.md](../concepts/reports-catalog.md) — Budget vs Actual  
- [reconcile-kpi-numbers.md](reconcile-kpi-numbers.md)
