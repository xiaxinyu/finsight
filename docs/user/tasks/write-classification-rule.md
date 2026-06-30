# Write a classification rule

| | |
| :--- | :--- |
| **Language** | English · [简体中文](write-classification-rule.zh-cn.md) |

Auto-assign a **category** when transaction text matches a pattern.

---

## Prerequisites

- Admin access.
- Target **category** exists with correct semantics ([set-category-semantics.md](set-category-semantics.md)).
- You know a stable keyword in descriptions (e.g. merchant name).

---

## Steps

1. Open **Admin → Rule engine** (`/app/admin/rules`).

2. Click **Add rule** (or edit an existing rule).

3. Set **pattern type**:
   - **Contains** — safest default.
   - **Equals** / **Starts with** — exact match.
   - **Regex** — advanced only.

4. Enter **pattern** (e.g. `STARBUCKS`).

5. Select **category** (L2).

6. Optional: run **impact preview** — check matched row count and amount.

7. Save. Enable the rule if there is a toggle.

8. Open **Transactions** → filter **Unclassified** — confirm matches decrease.

9. If Profile matters: **Profile → Refresh**.

---

## Verification

| Check | Expected |
| :--- | :--- |
| Impact preview | Reasonable match count (not zero, not entire ledger) |
| New imports / re-run | Matching rows get the category |
| Dashboard / reports | Totals shift to the correct bucket |

---

## Good practices

| Do | Avoid |
| :--- | :--- |
| Short, specific merchant tokens | Over-broad patterns like `PAY` |
| Preview before save | Regex without testing |
| One category per merchant family | Same pattern → conflicting categories |

---

## Troubleshooting

| Issue | Fix |
| :--- | :--- |
| Rule matches nothing | Try Contains; check case and bank description format |
| Rule matches too much | Narrow pattern; use Equals or longer substring |
| High risk / orphan in rule tree | Fix category link in Admin |

---

## Related

- [classify-unclassified-transactions.md](classify-unclassified-transactions.md)  
- [rules-guide.md](../../tech/contributing/rules-guide.md) — detailed syntax
