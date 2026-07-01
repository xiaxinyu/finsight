# Reconcile KPI numbers

| | |
| :--- | :--- |
| **Language** | English · [简体中文](reconcile-kpi-numbers.zh-cn.md) |

You will find why **Dashboard**, **bank app**, or **reports** show different totals and fix the root cause.

---

## Before you start

FinSight **Consumption** is **not** the same as bank “total spend”. See [data-semantics.md](../concepts/data-semantics.md).

---

## Step 1 — Align date range

| Page | Date rule |
| :--- | :--- |
| Dashboard | Uses **Period** picker (top-right) |
| Cashflow report | Same Period as Dashboard |
| Profile | Always **last 12 months** — do not compare to one-month Dashboard Net |

**Action:** Set Dashboard Period to match the report you compare.

---

## Step 2 — Align report scope

| If you compare… | Open | Scope |
| :--- | :--- | :--- |
| Living spend | Dashboard **Consumption** or Cashflow | Expense trend |
| All bank outflows | Bank app only | Includes transfers, loans, tax |
| Transfers / investments | **Transfers & Investments** | `non_pnl` |
| Tax | **Tax Summary** | `tax` |

---

## Step 3 — Check classification

1. Filter **Transactions** by Period and category.
2. Look for mislabeled rows (transfer counted as expense).
3. Fix via [set-category-semantics.md](set-category-semantics.md) or row edit.
4. **Profile → Refresh** if Profile looks wrong.

---

## Step 4 — Check data quality

| Signal | Action |
| :--- | :--- |
| High unclassified count | [classify-unclassified-transactions.md](classify-unclassified-transactions.md) |
| Profile **Reconciliation mismatch** | Wait for metric repair or re-import; classify first |
| Drill shows 0 rows but chart has slice | Hard refresh; v2.0.3+ trend matrix semantic drill; see [drill-down-from-reports.md](drill-down-from-reports.md) |

---

## Step 5 — Acceptance table

When fixed, these should hold (same Period, same user):

| Pair | Expected |
| :--- | :--- |
| Dashboard Net | ≈ Real income − Consumption on Dashboard |
| Dashboard vs Cashflow | Same Real income and Consumption totals |
| Consumption vs Budget vs Actual **Spent** | Same consumption scope |
| Transfer & Investments | Not included in Consumption |

---

## Still stuck?

| Role | Document |
| :--- | :--- |
| User | [reports-catalog.md](../concepts/reports-catalog.md) |
| Developer | [personal-finance-reporting-guide.md](../../tech/finance/personal-finance-reporting-guide.md) |

---

## Related docs

- [data-semantics.md](../concepts/data-semantics.md)  
- [refresh-profile.md](refresh-profile.md)
