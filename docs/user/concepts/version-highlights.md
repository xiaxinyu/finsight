# Version highlights (v2.0.0 → current)

| | |
| :--- | :--- |
| **Language** | English · [简体中文](version-highlights.zh-cn.md) |

> Full inventory: [function-list.md](../../tech/reference/function-list.md) · v2.0.2 plan: [v2.0.2-professional-finance-quality-plan.zh-cn.md](../../tech/roadmap/v2.0.2-professional-finance-quality-plan.zh-cn.md)

User-visible changes by release — use to align docs, UI, and expectations.

---

## v2.0.2 — Professional finance semantics (current)

<span style="color:#2563eb">**Theme**</span>: One semantic layer, materialized analytics, explainable reports.

### Data & metrics

| Feature | Description |
| :--- | :--- |
| Finance Semantic Layer | View `v_transaction_finance_semantics`: direction · economic_nature · semantic_tag · inclusion flags |
| Monthly semantic metrics | `REAL_INCOME` · `CONSUMPTION_EXPENSE` · `NET_CASHFLOW` in `fin_metric_monthly` |
| Profile materialization | `fin_profile_current`; GET read-only; POST Refresh recomputes |
| Semantic breakdown API | Aggregate by scope: expense · income · non_pnl · tax · refund |

### Dashboard & Profile

| Feature | Description |
| :--- | :--- |
| Dashboard semantic KPIs | Real income · Consumption · Net (replaces legacy month-income/expense) |
| Donut semantic breakdown | Top Reporting Classifications with drill |
| Profile 10 dimensions | Weighted score · confidence · user type · evidence per dimension |
| Metric hints | **?** on KPIs |

### Classification & transactions

| Feature | Description |
| :--- | :--- |
| Admin finance semantics | Category form: report_role · semantic tag · inclusion preview |
| Editable report_role | Persists to `cls_category`; taxonomy version bump |
| Transaction filters | Full Reporting Classification catalog + quick filters |
| Unclassified shortcuts | unclassified · data_quality entry points |

### Reports

| Feature | Description |
| :--- | :--- |
| Transfer & Finance | `non_pnl` scope |
| Tax Summary | `tax` scope |
| Budget vs Actual | Classification column; consumption-scope actual |
| Spending Drift | Two-period semantic buckets |
| Trend Changes | Reporting Classification YoY |
| Unified data quality bar | Top of all report pages |
| Unified Drill Drawer | Semantic drill to merchant layer |

### Terminology

| Before | Now |
| :--- | :--- |
| Non-P&L | Transfer · Finance · Investment |
| `non_pnl` scope label | Transfer & Finance |

---

## v2.0.1 — Quality optimization

| Feature | Description |
| :--- | :--- |
| Date-range SQL | Index-friendly half-open ranges; removed hot `year()` filters |
| Forecast | `hybrid_projection`; backtest metrics |
| Profile read path | GET no longer writes snapshot implicitly |
| CI | backend-test · sql-gate restored |

---

## v2.0.0 — Stability & quality gates

| Feature | Description |
| :--- | :--- |
| Metric gate | Reconciliation; degrade + repair on mismatch |
| Read-path stability | Unified report read paths and performance |
| L2 category seed | Asset, education, and other L2 additions |

---

## v1.8 — Classification governance

| Feature | Description |
| :--- | :--- |
| Rule impact preview | Count/amount before applying rule changes |
| Data quality layer | Unclassified and conflict visibility |
| Governance UX | Usage · rule coverage · report impact on categories |
| Report navigation | Decision-oriented report grouping |

---

## Recent commits (semantics & reports)

| Area | Change |
| :--- | :--- |
| `feat(semantics)` | Semantics catalog · category semantic picker · semanticTag persistence |
| `feat(analytics)` | Semantic breakdown API · period-summary · report structure alignment |
| `feat(drilldown)` | `drillParamsForSemanticTag` · category slice drill |
| `feat(reports)` | Layout polish · metric explanations on all KPIs |
| `fix(transactions)` | Auto-classify confirm write-back · drill legacy txnTypes conflict fix |

---

## Related docs

| Document | Purpose |
| :--- | :--- |
| [data-semantics.md](data-semantics.md) | How to read numbers |
| [reports-catalog.md](reports-catalog.md) | Report index |
| [finance-semantic-contract.zh-cn.md](../../tech/finance/finance-semantic-contract.zh-cn.md) | Semantic contract (ZH) |
