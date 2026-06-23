# Sprint 1 audit snapshot (2026-06-23)

Issue [#68](https://github.com/xiaxinyu/finsight/issues/68) — structure and remediation design docs. **CSV/JSON counts are not committed from a live DB**; populate locally:

```bash
chmod +x scripts/db/export-classification-audit-baseline.sh
RUN_TAG=2026-06-23-sprint1 ./scripts/db/export-classification-audit-baseline.sh
```

Then update `l2-category-candidates.zh-cn.md` `expected_rule_count` from Top100 exports.

See [../README.md](../README.md) for folder layout.
