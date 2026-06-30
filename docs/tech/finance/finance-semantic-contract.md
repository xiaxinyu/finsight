# FinSight finance semantic contract (v2.0.2)

| | |
| :--- | :--- |
| **Language** | English · [简体中文](finance-semantic-contract.zh-cn.md) |

> User guide: [data-semantics.md](../../user/concepts/data-semantics.md)

This contract defines how **each transaction** is interpreted in Dashboard, Profile, Forecast, and Reports.

**Rule for code:** Read inclusion/exclusion from view `v_transaction_finance_semantics` (or materialized metrics). Do **not** re-implement ad-hoc checks for `is_transfer`, `is_refund`, or `report_role` in each service.

---

## 1. Core dimensions

| Field | Values | Meaning |
| :--- | :--- | :--- |
| `cash_direction` | `inflow` · `outflow` · `neutral` | Direction of cash movement |
| `economic_nature` | `income` · `expense` · `transfer` · `refund` · `investment` · `liability` · `asset_adjustment` · `other` | Economic meaning |
| `report_role` | From `cls_category.report_role` | Which report family owns the row |
| `budget_behavior` | `fixed` · `variable` · `essential` · `unclassified` | Budget treatment |
| `quality_state` | `classified` · `inferred` · `unclassified` | Data trust |

---

## 2. Inclusion flags

| Flag | Rule (summary) |
| :--- | :--- |
| `include_in_income_trend` | Real income: inflow with `report_role=income`; not transfer or refund |
| `include_in_expense_trend` | Living spend: outflow expense; exclude transfer, refund, liability, investment, asset adjustment |
| `include_in_budget` | Budget buckets: fixed/budget spend; exclude transfer, refund, investment, liability |
| `include_in_cashflow` | Cashflow views: non-transfer flows |
| `include_in_profile` | Profile concentration: classified and non-transfer |

---

## 3. Typical scenarios

| Scenario | `economic_nature` | Income trend | Expense trend | Budget |
| :--- | :---: | :---: | :---: | :---: |
| Salary | `income` | Yes | No | No |
| Reimbursement inflow | `refund` | No | No | No |
| Purchase refund | `refund` | No | No | No |
| Credit card repayment | `liability` | No | No | No |
| Fund purchase | `investment` | No | No | No |
| Fund redemption | `investment` | No | No | No |
| Account transfer | `transfer` | No | No | No |
| Bank fee / interest | `expense` | No | By category | By category |
| Loan received | `liability` | No | No | No |
| Unclassified | `other` | No | No | No |

---

## 4. Implementation map

| Layer | Location |
| :--- | :--- |
| SQL view | `src/main/resources/db/migration/V32__transaction_finance_semantics.sql` (updated through V49) |
| Monthly metrics | `FinanceSemanticMetricsRepository` → `fin_metric_monthly` |
| Profile snapshot | `fin_profile_current` (`V33__fin_profile_current.sql`) |
| Profile GET | `/api/v1/analytics/profile` — read materialized row only |
| Profile refresh | `POST /api/v1/analytics/profile/refresh` — explicit recompute |
| Preferred metric codes | `REAL_INCOME`, `CONSUMPTION_EXPENSE` (fallback: `INCOME_TOTAL`, `EXPENSE_TOTAL`) |

---

## 5. Change policy

1. Any inclusion logic change → update this contract, the Flyway view, and tests.  
2. Do **not** count refunds, investment redemptions, or borrowing inflows as **Real income** by default.  
3. Do **not** count investment buys, loan repayments, or transfers as **Consumption** by default.

---

## 6. Related docs

| Document | Purpose |
| :--- | :--- |
| [data-semantics.md](../../user/concepts/data-semantics.md) | KPI definitions |
| [semantic-scenarios.md](../../user/concepts/semantic-scenarios.md) | Scenario lookup |
| [personal-finance-reporting-guide.md](./personal-finance-reporting-guide.md) | Developers / QA |
| [refresh-profile.md](../../user/tasks/refresh-profile.md) | Update Profile |
