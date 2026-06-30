# Semantic scenarios — quick reference

| | |
| :--- | :--- |
| **Language** | English · [简体中文](semantic-scenarios.zh-cn.md) |

One-page lookup for **how a row affects KPIs**. Full rules: [finance-semantic-contract.md](../../tech/finance/finance-semantic-contract.md).

---

## KPI columns

| Column | Meaning |
| :--- | :--- |
| **Income** | Counts in **Real income** |
| **Consumption** | Counts in **Consumption** |
| **Budget** | Counts in budget **Spent** |
| **Report** | Primary report to open |

---

## Scenarios

| Scenario | Income | Consumption | Budget | Report |
| :--- | :---: | :---: | :---: | :--- |
| Salary | Yes | No | No | Dashboard / Cashflow |
| Freelance income | Yes | No | No | Dashboard / Cashflow |
| Dining out | No | Yes | Yes | Budget vs Actual |
| Rent | No | Yes | Yes | Fixed vs Variable |
| Grocery | No | Yes | Yes | Budget vs Actual |
| Card → savings transfer | No | No | No | Transfer & Finance |
| Credit card repayment | No | No | No | Transfer & Finance |
| Buy fund / stock | No | No | No | Transfer & Finance |
| Sell fund / stock | No | No | No | Transfer & Finance |
| Purchase refund | No | No | No | Transactions (Refund) |
| Employer reimbursement | No | No | No | Transactions (Refund) |
| Income tax paid | No | No | No | Tax Summary |
| Tax refund | No | No | No | Tax Summary |
| Bank fee | No | Often | Often | By category tag |
| Unclassified | No | No | No | Fix in Transactions |

---

## If the table surprises you

1. Check **Reporting Classification** on the row.  
2. Check category semantics in Admin.  
3. See [reconcile-kpi-numbers.md](../tasks/reconcile-kpi-numbers.md).
