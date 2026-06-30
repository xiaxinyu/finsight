# Data semantics — how to read FinSight numbers

| | |
| :--- | :--- |
| **Language** | English · [简体中文](data-semantics.zh-cn.md) |

> Style: [_style-guide.md](_style-guide.md) · Engineering: [finance-semantic-contract.md](../../tech/finance/finance-semantic-contract.md)

---

## Why numbers differ from your bank app

Your bank shows **all outflows**. FinSight separates **living spend** from **transfers, loans, investments, and tax**.

FinSight does **not** add up raw statement lines. Each row gets a **finance semantic** (business meaning). Rules then decide which **KPI** (key performance indicator) includes that row.

---

## 1. Two layers of labels

| Layer | On screen | Answers | Examples |
| :--- | :--- | :--- | :--- |
| **Core — Transaction type** | Transaction type | Which cash bucket? | Income · Expense · Transfer · Finance · Tax · Refund |
| **Core — Reporting Classification** | Reporting Classification | Which report bucket? | Dining · Social · Housing · Loan · Investment |

**Core rule:** Dashboard, Profile, and reports group by **Reporting Classification + inclusion flags**, not by the bank label alone.

---

## 2. Three headline KPIs

| KPI | Plain English | **Included** | **Not included** |
| :--- | :--- | :--- | :--- |
| **Real income** | Money you earned | Salary, business income | Refunds, reimbursements, selling investments, borrowed cash |
| **Consumption** | Living spend | Food, rent, daily costs in budget scope | Transfers, credit-card repayments, buying investments, refunds |
| **Net cashflow** | What is left | Real income − Consumption | — |

**Data path:** database view `v_transaction_finance_semantics` → monthly codes `REAL_INCOME` and `CONSUMPTION_EXPENSE`.

**In the UI:** hover **?** next to a Dashboard KPI for the same text.

---

## 3. Flows you view separately

| Flow | Open this report | Why |
| :--- | :--- | :--- |
| Moving money between your accounts | Fund Flow · Transfer & Finance | Not living expense |
| Loan drawdown / repayment | Transfer & Finance | Balance sheet (liability), not consumption |
| Buy / sell investments | Transfer & Finance | Asset allocation |
| Tax paid / tax refund | Tax Summary | Separate from daily budget |
| Refunds & reimbursements | Transactions → Refund filter | Not counted as salary income |

**Note:** Bank “monthly spend” is often **higher** than Dashboard **Consumption**. Different scope — not a software bug.

---

## 4. Data quality

| State | Meaning | What you should do |
| :--- | :--- | :--- |
| Classified | Category is set | OK for reports |
| Inferred | Assigned by a rule | Check large amounts |
| Unclassified | No category | Classify first; then refresh Profile |

More unclassified rows → lower **Data trust** on Dashboard and Profile.

---

## 5. Numbers do not match — checklist

1. **Same date range?** Dashboard uses your Period picker. Profile always uses the **last 12 months**.
2. **Same report scope?** Spending report ≠ transfer report ≠ tax report.
3. **Categories changed?** After Admin edits, click **Profile → Refresh**.
4. **Stale snapshot?** Profile shows *Stale* until you refresh. See [dashboard-profile.md](dashboard-profile.md).

---

## 6. Processing pipeline

```
Bank row
  → Transaction type
  → Reporting Classification (semantic tag)
  → Inclusion flags (income trend? expense trend? budget?)
  → KPI / Report / Profile score
```

---

## 7. Related documents

| Document | Purpose |
| :--- | :--- |
| [dashboard-profile.md](dashboard-profile.md) | Dashboard & Profile |
| [reports-catalog.md](reports-catalog.md) | All reports |
| [version-highlights.md](version-highlights.md) | v2.0.x features |
| [semantic-scenarios.md](semantic-scenarios.md) | KPI scenario lookup |
| [personal-finance-reporting-guide.md](../../tech/finance/personal-finance-reporting-guide.md) | APIs (developers) |
