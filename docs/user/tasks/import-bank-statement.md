# Import a bank statement

| | |
| :--- | :--- |
| **Language** | English · [简体中文](import-bank-statement.zh-cn.md) |

You will upload a bank or card export file and load transactions into FinSight.

---

## Prerequisites

- FinSight is running (`mvn spring-boot:run` or deployed instance).
- You have an export file from your bank (CSV/Excel format supported by your bank adapter).
- You know which **bank card / account** the file belongs to.

---

## Steps

1. Open **Transactions → Import** (`/app/statements/upload`).

2. Select the **bank** and **account** (card) that match the file.

3. Choose the export file and start upload.

4. Wait for the import to finish. Check for error messages in the UI.

5. Open **Transactions** (`/app/transactions`):
   - Confirm new rows appear with correct dates and amounts.
   - Filter **Unclassified** if the import left categories empty.

6. Optional: open **Import history** (`/app/statements`) to see past uploads.

---

## Verification

| Check | Expected |
| :--- | :--- |
| Transaction count | Increases by expected number of rows |
| Dates | Match the statement period |
| Amounts | Match source file (spot-check 3–5 rows) |
| Dashboard | After setting Period, KPIs reflect new data |

---

## After import

| Next step | Guide |
| :--- | :--- |
| Classify new rows | [classify-unclassified-transactions.md](classify-unclassified-transactions.md) |
| Update Profile | [refresh-profile.md](refresh-profile.md) |
| Fix wrong categories | [set-category-semantics.md](set-category-semantics.md) |

---

## Cleanup (optional)

If the import was wrong, use your usual admin/import rollback workflow or delete the erroneous batch per your operational policy (not covered here).

---

## Related docs

- [getting-started.md](../concepts/getting-started.md)  
- [local-development.md](../setup/local-development.md)
