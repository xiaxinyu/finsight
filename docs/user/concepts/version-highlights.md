# Version highlights (v2.0.0 → current)

| | |
| :--- | :--- |
| **Language** | English · [简体中文](version-highlights.zh-cn.md) |

> Full feature list: [function-list.md](../../tech/reference/function-list.md)

User-visible changes by release. Use this page to match the UI with documentation.

---

## v2.0.3 — Year-over-year trends & navigation *(current)*

**Theme:** Income / consumption / debt YoY analytics and a clearer Reports sidebar.

| Area | What changed |
| :--- | :--- |
| **YoY reports** | Income Trends · Consumption Trends (redesign) · Debt Trends |
| **API** | `/analytics/income-trends` · `/analytics/debt-trends` · enhanced `/analytics/trends` |
| **Navigation** | Six groups: Monthly overview · YoY trends · Spending analysis · Capital & taxes · Forecast & risk · Merchants |
| **Names** | Period Comparison · Transfers & Investments · Top Merchants / Merchant Changes |
| **Drill-down** | Fixed semantic matrix drill returning zero rows; breadcrumbs match menu |
| **Docs** | [reports-catalog.md](reports-catalog.md) · [v2.0.3 release notes](../../tech/ops/v2.0.3-release-notes.md) |

---

## v2.0.2 — Professional finance semantics

**Theme:** One semantic layer for Dashboard, Profile, and all reports.

| Area | What changed |
| :--- | :--- |
| **Semantic layer** | View `v_transaction_finance_semantics`; monthly KPIs `REAL_INCOME`, `CONSUMPTION_EXPENSE` |
| **Profile** | Materialized snapshot; **Refresh** to recompute; 10 dimensions + confidence |
| **Dashboard** | Semantic KPIs; donut by Reporting Classification; drill-down by semantic tag |
| **Categories** | Admin shows finance semantics; editable `report_role` and semantic tags |
| **Transactions** | Filter by full Reporting Classification catalog |
| **Reports** | Transfer & Finance, Tax Summary, unified data-quality bar, metric **?** hints |
| **Terms** | Plain labels: Transfer · Finance · Investment (not “Non-P&L”) |

---

## v2.0.1 — Quality

| Item | Benefit |
| :--- | :--- |
| Index-friendly date SQL | Faster reports on large ledgers |
| Forecast `hybrid_projection` | Better forward view |
| Profile read path | GET no longer writes snapshot silently |

---

## v2.0.0 — Stability

| Item | Benefit |
| :--- | :--- |
| Metric gate | Detects mismatch between stored metrics and live data |
| Read-path cleanup | Consistent report loading |

---

## v1.8 — Classification governance

Rule impact preview · data-quality layer · decision-oriented report navigation.

---

## Related docs

| Doc | Purpose |
| :--- | :--- |
| [data-semantics.md](data-semantics.md) | Read KPIs |
| [reports-catalog.md](reports-catalog.md) | Report index |
