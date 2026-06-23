# Classification audit baseline template

Use this template when running [classification-data-audit.sql](./classification-data-audit.sql) **before and after** each governance sprint. Store exports under a dated folder, e.g. `audit-baselines/2026-06-23-pre/`.

## Run metadata

| Field | Value |
|-------|-------|
| Date (UTC/local) | |
| Operator | |
| Database / environment | e.g. `finsight@localhost` |
| Git commit / release | e.g. `v1.8.0-beta.1` |
| Phase | `BEFORE` / `AFTER` |
| Sprint / issue | e.g. `#68` |

## §20 Baseline summary (paste query result)

| Metric | BEFORE | AFTER | Target |
|--------|--------|-------|--------|
| `active_orphan_rules` | | | 0 |
| `active_invalid_pattern_rules` | | | 0 |
| `category_field_drift_rows` | | | 0 |
| `unclassified_txns` | | | ↓ |
| `other_category_txns` | | | ↓ |
| `merchant_profile_mismatch_count` | | | 0 |

## §9 Unclassified coverage

| total_txns | unclassified_txns | unclassified_pct | unclassified_amount |
|------------|-------------------|------------------|---------------------|
| | | | |

## Exported files (attach or link)

- [ ] `baseline-unclassified-top100.csv` — audit §18
- [ ] `baseline-other-consumption-top100.csv` — audit §19
- [ ] `baseline-orphan-rules.csv` — audit §3
- [ ] `baseline-invalid-rules.csv` — audit §4
- [ ] `baseline-field-drift.csv` — audit §11
- [ ] `baseline-merchant-token-samples.csv` — audit §21

## Remediation decisions

| Item type | ID / key | Decision | Script / action | Done |
|-----------|----------|----------|-----------------|------|
| Orphan rule | | archive / remap | orphan-rules-remediation.sql Step 3 | |
| Invalid rule | | archive / restore pattern | invalid-rules-remediation.sql | |
| Field drift txn | | sync from consume_code | transaction-category-field-remediation.sql | |
| OTHER txn | | new rule / recategorize | Rule Engine + optional batch | |

## Sign-off

- [ ] BEFORE baseline saved
- [ ] Remediation scripts reviewed (not auto-run)
- [ ] AFTER baseline saved and compared
- [ ] `verify-schema-migration` OK

Notes:

---

Workflow reference: [classification-governance-workflow.zh-cn.md](./classification-governance-workflow.zh-cn.md)
