# Personal finance reporting reference (technical)

| | |
| :--- | :--- |
| **Language** | English · [简体中文](personal-finance-reporting-guide.zh-cn.md) |

> **User guides:** [data-semantics.md](../../user/concepts/data-semantics.md) · [reports-catalog.md](../../user/concepts/reports-catalog.md)  
> **Contract:** [finance-semantic-contract.zh-cn.md](./finance-semantic-contract.zh-cn.md)

For engineering, QA, and acceptance — APIs, views, metric codes, consistency rules.

---

## 1. Data sources

| Consumer | Primary source | Fallback |
| :--- | :--- | :--- |
| Dashboard KPIs | `GET /api/v1/analytics/metrics/period-summary` | Transaction aggregates |
| Dashboard donut | `GET /api/v1/analytics/semantic-breakdown?scope=expense` | — |
| Profile | Materialized `fin_profile_current` | `fin_metric_monthly` |
| Reports | semantic-breakdown / per-report services | See mappers |
| Transaction filters | `v_transaction_finance_semantics` | — |

Canonical view: `v_transaction_finance_semantics` (Flyway V32+, V49 tag-driven inclusion).

---

## 2. Headline KPI mapping

| UI label | MetricCode | Inclusion |
| :--- | :--- | :--- |
| Real income | `REAL_INCOME` | `include_in_income_trend = 1` |
| Consumption | `CONSUMPTION_EXPENSE` | `include_in_expense_trend = 1` |
| Net | `NET_CASHFLOW` | Derived or materialized |

---

## 3. Semantic breakdown scopes

| scope | SQL predicate (summary) |
| :--- | :--- |
| `expense` | `include_in_expense_trend = 1` |
| `income` | `include_in_income_trend = 1` |
| `non_pnl` | transfer / investment / liability flows |
| `tax` | `tax_expense` · `tax_refund` tags |
| `refund` | `refund_reimbursement` |

Implementation: `SemanticBreakdownRepository.java`

---

## 4. Drill-down

| Endpoint | Notes |
| :--- | :--- |
| `GET /api/v1/transactions/drill-breakdown` | Category · merchant · sample transactions |

**v2.0.2+**: When `semanticFilter` is set, `TransactionMapper.filterTxnTypesT` skips legacy `txn_types` filters.

---

## 5. Profile endpoints

| Endpoint | Behavior |
| :--- | :--- |
| `GET /api/v1/analytics/profile` | Read `fin_profile_current` only |
| `POST /api/v1/analytics/profile/refresh` | Recompute and materialize |

Metrics prefer `REAL_INCOME` / `CONSUMPTION_EXPENSE`; fallback `INCOME_TOTAL` / `EXPENSE_TOTAL`.

Runbook: [profile-materialization-runbook.zh-cn.md](./profile-materialization-runbook.zh-cn.md)

---

## 6. Consistency acceptance

1. Dashboard period Net ≈ Cashflow report for same period  
2. Budget vs Actual **Spent** uses consumption scope  
3. Transfer & Finance totals must not appear in Consumption  
4. Profile Refresh updates `asOf`; stale banner clears  
5. Metric gate mismatch shows UI warning; no silent inline recompute on read

---

## 7. Frontend hint constants

| File | Constants |
| :--- | :--- |
| `frontend/src/components/MetricExplanation.tsx` | `DASHBOARD_METRIC_HINTS` · `REPORT_METRIC_HINTS` |
| `frontend/src/utils/reportTaxonomy.ts` | `REPORT_METRICS_SOURCE` · scope labels |

---

## 8. Change rules

1. Inclusion logic changes → update contract, Flyway view, tests  
2. Do not default refunds / redemptions / borrowing into income trend  
3. Do not default repayments / transfers / investment buys into expense trend
