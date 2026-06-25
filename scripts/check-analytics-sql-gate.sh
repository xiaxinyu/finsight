#!/usr/bin/env bash
# v2.0.1 gate: block non-sargable date filters in analytics WHERE clauses.
# v2.0.2 gate: block legacy is_transfer/is_refund expense filters (use semantic view).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/src/main/java/com/finsight/application/analytics"
DATE_PATTERN='where[^;]*\byear\s*\(|where[^;]*\bdate_format\s*\('
LEGACY_SEMANTIC_PATTERN='is_transfer\s*=\s*0.*is_refund\s*=\s*0|is_refund\s*=\s*0.*is_transfer\s*=\s*0'

find_violations() {
  local pattern="$1"
  if command -v rg >/dev/null 2>&1; then
    rg -n -i "$pattern" "$TARGET" --glob '*.java' || true
  else
    grep -rEni "$pattern" "$TARGET" --include='*.java' || true
  fi
}

date_violations=$(find_violations "$DATE_PATTERN")
legacy_violations=$(find_violations "$LEGACY_SEMANTIC_PATTERN")

if [[ -n "$date_violations" ]]; then
  echo "Non-sargable date filter in analytics SQL:"
  echo "$date_violations"
  exit 1
fi

if [[ -n "$legacy_violations" ]]; then
  echo "Legacy expense filter in analytics SQL (use v_transaction_finance_semantics.include_in_expense_trend):"
  echo "$legacy_violations"
  exit 1
fi

echo "Analytics SQL gate: OK (no year()/date_format() in WHERE; no legacy is_transfer/is_refund filters)"
