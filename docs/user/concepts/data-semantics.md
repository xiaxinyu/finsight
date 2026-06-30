# Data semantics: how to read FinSight numbers

| | |
| :--- | :--- |
| **Language** | English · [简体中文](data-semantics.zh-cn.md) |

> Markers: [_style-guide.md](_style-guide.md) · Implementation: [finance-semantic-contract.zh-cn.md](../../tech/finance/finance-semantic-contract.zh-cn.md)

FinSight does **not** sum raw bank feeds. Each transaction receives a **finance semantic**, then inclusion rules decide which KPIs it affects.

---

## 1. Two-layer taxonomy

| Layer | UI label | Question answered | Examples |
| :--- | :--- | :--- | :--- |
| <span style="color:#2563eb">**Transaction type**</span> | Transaction type | Cash-direction bucket | Income · Expense · Transfer · Finance · Tax · Refund |
| <span style="color:#2563eb">**Reporting Classification**</span> | Reporting Classification | Analytic bucket | Dining · Social · Housing · Loan · Investment |

<span style="color:#2563eb">**Core rule**</span>: Dashboard, Profile, and reports aggregate by **Reporting Classification + inclusion flags**, not by the bank’s debit/credit label alone.

---

## 2. Three headline KPIs

| KPI | <span style="color:#059669">Included</span> | <span style="color:#d97706">Excluded</span> |
| :--- | :--- | :--- |
| **Real income** | Salary, operating income (`report_role=income`) | Refunds, reimbursements, investment redemptions, borrowing inflows |
| **Consumption** | Living and budget-tracked spend | Transfers, card repayments, investment buys, refunds |
| **Net cashflow** | Real income − Consumption | — |

**Source**: view `v_transaction_finance_semantics` → monthly codes `REAL_INCOME` / `CONSUMPTION_EXPENSE`.

Hover **?** on Dashboard KPIs for the same definitions (`DASHBOARD_METRIC_HINTS`).

---

## 3. Non-living cash flows (view separately)

| Flow | Where to look | Why separate |
| :--- | :--- | :--- |
| Account transfers | Fund Flow · Transfer & Finance | Not consumption pressure |
| Loans / repayments | Transfer & Finance | Liability, not living expense |
| Investment trades | Transfer & Finance | Asset allocation |
| Tax | Tax Summary | Isolated from daily spending decisions |
| Refunds / reimbursements | Transactions → Refund filter | Offsets or inflows; not income trend |

<span style="color:#d97706">**Note**</span>: Your bank app’s “monthly spend” is usually **higher** than Dashboard **Consumption** — scope difference, not a bug.

---

## 4. Data quality and trust

| `quality_state` | Meaning | Action |
| :--- | :--- | :--- |
| `classified` | Category confirmed | Safe for reports |
| `inferred` | Rule-assigned | Spot-check large amounts |
| `unclassified` | Missing category | Classify before trusting Profile |

More unclassified rows → lower **Data trust** on Dashboard and Profile.

---

## 5. When numbers disagree — checklist

1. **Period** — Dashboard uses the selected range; Profile uses the **last 12 months** fixed window  
2. **Scope** — Spending report vs transfer report vs tax report  
3. **Classification** — After Admin category / semantic tag changes, **Refresh Profile**  
4. **Stale metrics** — Profile **Refresh**; reconciliation banner → [dashboard-profile.md](dashboard-profile.md)

---

## 6. Mental model (quick)

```
Raw transaction
    → Transaction type (direction bucket)
    → Reporting Classification (semantic_tag)
    → Inclusion flags (income trend? expense trend? budget?)
    → KPI / Report / Profile dimension
```

---

## 7. Related docs

| Document | Purpose |
| :--- | :--- |
| [dashboard-profile.md](dashboard-profile.md) | Dashboard & Profile reading guide |
| [reports-catalog.md](reports-catalog.md) | Report purposes and KPIs |
| [version-highlights.md](version-highlights.md) | v2.0.x feature evolution |
| [personal-finance-reporting-guide.md](../../tech/finance/personal-finance-reporting-guide.md) | APIs and acceptance (engineering) |
