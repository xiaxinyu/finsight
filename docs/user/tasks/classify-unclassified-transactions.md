# Classify unclassified transactions

| | |
| :--- | :--- |
| **Language** | English · [简体中文](classify-unclassified-transactions.zh-cn.md) |

You will assign categories to rows that have no **Reporting Classification**, so Dashboard and Profile **Data trust** improve.

---

## Prerequisites

- Transactions are imported ([import-bank-statement.md](import-bank-statement.md)).
- You understand [Real income vs Consumption](../concepts/data-semantics.md).

---

## Steps

1. Open **Transactions** (`/app/transactions`).

2. Apply filter **Unclassified** (or **Data quality** quick filter).

3. Sort by **amount** descending — fix large rows first.

4. For each row (or batch):
   - Pick the correct **category** (L2).
   - Confirm **Transaction type** matches intent (Expense, Transfer, etc.).
   - Save.

5. Optional: create a **rule** for repeat merchants — [write-classification-rule.md](write-classification-rule.md).

6. When the unclassified count is low, open **Dashboard** and check the data-quality strip.

7. **Profile → Refresh** ([refresh-profile.md](refresh-profile.md)).

---

## Verification

| Check | Expected |
| :--- | :--- |
| Unclassified filter | Row count drops |
| Dashboard data quality | Unclassified count decreases |
| Donut / reports | Totals stable; slices move to correct buckets |
| Profile **Data trust** | Score improves after Refresh |

---

## Tips

| Situation | Category choice |
| :--- | :--- |
| Money between your own accounts | Transfer (not Dining) |
| Credit card payment | Finance / liability — not Consumption |
| Salary | Income category with `report_role=income` |
| Tax payment | Tax-related category → appears in Tax Summary |

Wrong category semantics? See [set-category-semantics.md](set-category-semantics.md).

---

## Related docs

- [set-category-semantics.md](set-category-semantics.md)  
- [reconcile-kpi-numbers.md](reconcile-kpi-numbers.md)
