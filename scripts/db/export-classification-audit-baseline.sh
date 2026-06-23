#!/usr/bin/env bash
# Export classification audit baseline CSV/TSV into docs/tech/database/audit-results/<run-tag>/.
# Does NOT mutate data — read-only SELECT exports.
#
# Prerequisites: mysql client; finsight DB with classification tables.
#
# Usage:
#   ./scripts/db/export-classification-audit-baseline.sh
#   RUN_TAG=2026-06-23-sprint1 MYSQL_PASSWORD=secret ./scripts/db/export-classification-audit-baseline.sh
#
# After export, fill l2-category-candidates.zh-cn.md expected_rule_count from Top100 CSVs.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
EXPORT_DIR="${ROOT}/docs/tech/database/audit-exports"
RUN_TAG="${RUN_TAG:-$(date +%Y-%m-%d)-sprint1}"
OUT_DIR="${ROOT}/docs/tech/database/audit-results/${RUN_TAG}"

HOST="${MYSQL_HOST:-127.0.0.1}"
PORT="${MYSQL_PORT:-3306}"
USER="${SPRING_DATASOURCE_USERNAME:-${MYSQL_USER:-root}}"
PASS="${SPRING_DATASOURCE_PASSWORD:-${MYSQL_PASSWORD:-123456}}"
DB="${MYSQL_DATABASE:-finsight}"

MYSQL=(mysql -h"$HOST" -P"$PORT" -u"$USER" -p"$PASS" "$DB" -N -B)

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql client not found. Install mysql or run queries manually from docs/tech/database/audit-exports/" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

export_csv() {
  local sql_file="$1"
  local out_csv="$2"
  if [[ ! -f "$sql_file" ]]; then
    echo "Missing SQL: $sql_file" >&2
    exit 1
  fi
  echo "Exporting $(basename "$out_csv") ..."
  "${MYSQL[@]}" < "$sql_file" | sed 's/\t/,/g' > "$out_csv"
}

export_csv "${EXPORT_DIR}/03-orphan-rules.sql" "${OUT_DIR}/baseline-orphan-rules.csv"
export_csv "${EXPORT_DIR}/04-invalid-rules.sql" "${OUT_DIR}/baseline-invalid-rules.csv"
export_csv "${EXPORT_DIR}/06-duplicate-patterns.sql" "${OUT_DIR}/baseline-duplicate-patterns.csv"
export_csv "${EXPORT_DIR}/11-field-drift.sql" "${OUT_DIR}/baseline-field-drift.csv"
export_csv "${EXPORT_DIR}/18-unclassified-top100.sql" "${OUT_DIR}/baseline-unclassified-top100.csv"
export_csv "${EXPORT_DIR}/19-other-top100.sql" "${OUT_DIR}/baseline-other-consumption-top100.csv"
export_csv "${EXPORT_DIR}/21-merchant-token-samples.sql" "${OUT_DIR}/baseline-merchant-token-samples.csv"

echo "Exporting baseline-summary.json ..."
SUMMARY_TSV=$("${MYSQL[@]}" < "${EXPORT_DIR}/20-baseline-summary.sql")
IFS=$'\t' read -r _artifact orphan invalid drift unclassified other merchant <<< "$SUMMARY_TSV"
cat > "${OUT_DIR}/baseline-summary.json" <<EOF
{
  "artifact": "BASELINE_SUMMARY",
  "runTag": "${RUN_TAG}",
  "exportedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "database": "${DB}@${HOST}:${PORT}",
  "active_orphan_rules": ${orphan:-0},
  "active_invalid_pattern_rules": ${invalid:-0},
  "category_field_drift_rows": ${drift:-0},
  "unclassified_txns": ${unclassified:-0},
  "other_category_txns": ${other:-0},
  "merchant_profile_mismatch_count": ${merchant:-0}
}
EOF

cat > "${OUT_DIR}/manifest.json" <<EOF
{
  "runTag": "${RUN_TAG}",
  "issue": "#68",
  "phase": "BEFORE",
  "exports": [
    "baseline-orphan-rules.csv",
    "baseline-invalid-rules.csv",
    "baseline-duplicate-patterns.csv",
    "baseline-field-drift.csv",
    "baseline-unclassified-top100.csv",
    "baseline-other-consumption-top100.csv",
    "baseline-merchant-token-samples.csv",
    "baseline-summary.json"
  ],
  "remediationDocs": [
    "l2-category-candidates.zh-cn.md",
    "rule-fix-priority-list.zh-cn.md"
  ],
  "apiSummary": "GET /api/v1/maintenance/classification-audit-summary"
}
EOF

echo "Done. Files written to ${OUT_DIR}"
echo "Next: update l2-category-candidates.zh-cn.md expected_rule_count from Top100 CSVs."
