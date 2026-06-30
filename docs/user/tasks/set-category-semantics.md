# Set category semantics (Admin)

| | |
| :--- | :--- |
| **Language** | English · [简体中文](set-category-semantics.zh-cn.md) |

You will set **report role** and **semantic tag** on a category so Dashboard KPIs and reports use the correct scope.

---

## Prerequisites

- Admin access.
- You know whether the category is living spend, income, transfer, tax, etc. ([data-semantics.md](../concepts/data-semantics.md)).

---

## Steps

1. Open **Admin → Categories** (`/app/admin/categories`).

2. Select the category (usually an L2 row).

3. In the form, find **Finance semantics**:
   - **Transaction type** (`txn_types`)
   - **Report role**
   - **Reporting Classification** (semantic tag)
   - Inclusion preview (income trend / expense trend / budget)

4. Set values that match real-world meaning, for example:
   - Dining → expense trend **on**, semantic tag `dining_spending`
   - Salary → income trend **on**, report role **income**
   - Card repayment → expense trend **off**, nature **liability**

5. **Save**. Taxonomy version may bump (expected).

6. Optional: **Sync transactions** if the admin UI offers cascade to existing rows.

7. Open **Transactions** — spot-check affected rows.

8. **Dashboard** — confirm Consumption / Real income for your Period.

9. **Profile → Refresh**.

---

## Verification

| Check | Expected |
| :--- | :--- |
| Semantic preview | Inclusion flags match your intent |
| Dashboard KPI | Rows move in/out of Consumption or Real income correctly |
| Tax Summary / Transfer & Finance | Rows appear only in the right report |
| Profile | Data trust stable or improved after Refresh |

---

## Do not

- Mark transfers or loan repayments as normal **Consumption** unless that is truly your policy.
- Mark refunds or reimbursements as **Real income**.

Details: [finance-semantic-contract.md](../../tech/finance/finance-semantic-contract.md).

---

## Related docs

- [classify-unclassified-transactions.md](classify-unclassified-transactions.md)  
- [reconcile-kpi-numbers.md](reconcile-kpi-numbers.md)
