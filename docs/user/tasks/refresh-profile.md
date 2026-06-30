# Refresh your Financial Profile

| | |
| :--- | :--- |
| **Language** | English · [简体中文](refresh-profile.zh-cn.md) |

You will rebuild the **12-month Financial Profile** snapshot after you import data or change categories.

---

## Prerequisites

- You are logged in.
- You have imported transactions (see [import-bank-statement.md](import-bank-statement.md)).
- Optional but recommended: classify unclassified rows ([classify-unclassified-transactions.md](classify-unclassified-transactions.md)).

---

## Steps

1. Open **Profile** in the left menu (`/app/profile`).

2. Read the banner at the top:
   - **Profile snapshot not ready** → no snapshot exists yet.
   - **Profile may be outdated** → data changed since last run.

3. Click the button in the page header:
   - **Generate profile** — first time.
   - **Refresh** — update an existing snapshot.

4. Wait until the radar chart and overall score load (usually a few seconds).

5. Review:
   - **Overall score** and **Confidence**
   - **Weakest** dimensions (improvement areas)
   - Click a dimension for **Reason** and **Evidence**

---

## Verification

| Check | Expected |
| :--- | :--- |
| Banner | No “not ready” or “outdated” message (unless data changed again) |
| **asOf** date | Shows today or recent date |
| Radar chart | Ten dimensions with scores 0–100 |
| Data trust | Higher after you classify more rows |

---

## When to refresh

| Event | Action |
| :--- | :--- |
| New statement import | Refresh |
| Category or semantic tag change | Refresh |
| Rule batch reclassification | Refresh |
| Monthly review | Refresh if stale banner appears |

---

## Troubleshooting

| Issue | Action |
| :--- | :--- |
| **Reconciliation mismatch** warning | Classify data; see [reconcile-kpi-numbers.md](reconcile-kpi-numbers.md) |
| Low **Data trust** | [classify-unclassified-transactions.md](classify-unclassified-transactions.md) |
| Score unchanged but data changed | Confirm Refresh finished; hard-reload page |

---

## Related docs

- [dashboard-profile.md](../concepts/dashboard-profile.md) — how to read Profile  
- [finance-semantic-contract.md](../../tech/finance/finance-semantic-contract.md) — scoring inputs
