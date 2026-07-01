# Personal finance reporting reference

| | |
| :--- | :--- |
| **Language** | English · [简体中文](personal-finance-reporting-guide.zh-cn.md) |

> **User guides:** [data-semantics.md](../../user/concepts/data-semantics.md) · [reports-catalog.md](../../user/concepts/reports-catalog.md)  
> **Semantic rules:** [finance-semantic-contract.md](./finance-semantic-contract.md) · [中文](./finance-semantic-contract.zh-cn.md)

For developers and QA: APIs, database views, metric codes, and acceptance checks.

---

## 1. Data sources

| UI area | API / table | Fallback |
| :--- | :--- | :--- |
| Dashboard KPIs | `GET /api/v1/analytics/metrics/period-summary` | Transaction sum |
| Dashboard donut | `GET /api/v1/analytics/semantic-breakdown?scope=expense` | — |
| Profile | `fin_profile_current` (materialized) | `fin_metric_monthly` |
| Reports | Per-report service + semantic breakdown | See mappers |
| Transaction filters | `v_transaction_finance_semantics` | — |

Canonical SQL view: `v_transaction_finance_semantics` (Flyway V32+, V49 inclusion by tag).

---

## 2. Headline KPI codes

| UI label | `MetricCode` | Inclusion rule |
| :--- | :--- | :--- |
| Real income | `REAL_INCOME` | `include_in_income_trend = 1` |
| Consumption | `CONSUMPTION_EXPENSE` | `include_in_expense_trend = 1` |
| Net | `NET_CASHFLOW` | Derived or materialized |

---

## 3. Semantic breakdown scopes

| `scope` param | Meaning |
| :--- | :--- |
| `expense` | Living spend trend |
| `income` | Real income trend |
| `non_pnl` | Transfers, loans, investments |
| `tax` | Tax paid and refunds |
| `refund` | Reimbursements |

Code: `SemanticBreakdownRepository.java`

---

## 4. Drill-down API

`GET /api/v1/transactions/drill-breakdown`

When `semanticFilter` is set, legacy `txn_types` filters are **skipped** (`TransactionMapper.filterTxnTypesT`) so semantic tags are not blocked by old category types.

---

## 5. Profile API

| Method | Path | Behavior |
| :--- | :--- | :--- |
| GET | `/api/v1/analytics/profile` | Read materialized snapshot only |
| POST | `/api/v1/analytics/profile/refresh` | Recompute and save |

Prefer `REAL_INCOME` / `CONSUMPTION_EXPENSE`; fallback `INCOME_TOTAL` / `EXPENSE_TOTAL`.

---

## 6. Acceptance checklist

1. Dashboard Net ≈ Cashflow report (same period)  
2. Budget vs Actual **Spent** uses consumption scope  
3. Transfer & Investments totals ∉ Consumption  
4. Profile Refresh updates `asOf`  
5. Metric gate mismatch shows UI warning (no silent recompute on GET)  
6. v2.0.3+: Consumption / Income / Debt Trends matrix drill returns rows when slice has amount

---

## 7. Frontend constants

| File | Content |
| :--- | :--- |
| `MetricExplanation.tsx` | `DASHBOARD_METRIC_HINTS`, `REPORT_METRIC_HINTS` |
| `reportTaxonomy.ts` | Scope labels, filter catalog |

---

## 8. Change policy

1. Inclusion logic changes → update contract, Flyway view, tests  
2. Never default refunds / redemptions / borrowing into income trend  
3. Never default repayments / transfers / investment buys into expense trend

---

## 9. Year-over-year trend APIs (v2.0.3)

| Report | Path | Service |
| :--- | :--- | :--- |
| Consumption Trends | `GET /api/v1/analytics/trends` | `TrendAnalysisService` |
| Income Trends | `GET /api/v1/analytics/income-trends` | `IncomeTrendAnalysisService` |
| Debt Trends | `GET /api/v1/analytics/debt-trends` | `DebtTrendAnalysisService` |

**Query params:** `fromYear`, `toYear`, optional `historyFromYear` (matrix column start).

**Inclusion:**

| Report | SQL inclusion |
| :--- | :--- |
| Income Trends | `include_in_income_trend = 1` |
| Consumption Trends | `include_in_expense_trend = 1` |
| Debt Trends | Liability semantic tags (borrowing inflow / repayment outflow) |

**Drill-down (v2.0.3):** When `semanticFilter` is set, do **not** also send `consumeName` (display label). Frontend: `buildDrillContext.ts`, `UnifiedDrillDrawer.tsx`.

**Nav config:** `frontend/src/config/reportNavigation.ts` — menu groups and breadcrumbs.

Release notes: [v2.0.3-release-notes.md](../ops/v2.0.3-release-notes.md)
