# Classification audit results

Dated snapshots from [classification-data-audit.sql](../classification-data-audit.sql) exports. **No auto-remediation** — CSV/JSON here are read-only baselines; apply fixes via manual SQL in `docs/tech/database/`.

## Generate a snapshot

```bash
chmod +x scripts/db/export-classification-audit-baseline.sh
RUN_TAG=2026-06-23-sprint1 ./scripts/db/export-classification-audit-baseline.sh
```

Or query live counts via API (requires logged-in session):

`GET /api/v1/maintenance/classification-audit-summary`

## Folder layout

| Path | Purpose |
|------|---------|
| `{run-tag}/manifest.json` | Export manifest + issue link |
| `{run-tag}/baseline-*.csv` | §3–§21 audit extracts |
| `{run-tag}/baseline-summary.json` | §20 summary counts |
| `{run-tag}/l2-category-candidates.zh-cn.md` | Proposed L2 categories with `report_role` |
| `{run-tag}/rule-fix-priority-list.zh-cn.md` | P0/P1/P2 remediation backlog |

Template sprint folder: [2026-06-23-sprint1](./2026-06-23-sprint1/) (structure + design docs; run export script locally to populate CSV counts).

Workflow: [classification-governance-workflow.zh-cn.md](../classification-governance-workflow.zh-cn.md)
