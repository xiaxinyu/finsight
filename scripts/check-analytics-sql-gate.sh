#!/usr/bin/env bash
# v2.0.1 gate: block non-sargable date filters in analytics WHERE clauses.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$ROOT/src/main/java/com/finsight/application/analytics"
PATTERN='where[^;]*\byear\s*\(|where[^;]*\bdate_format\s*\('

find_violations() {
  if command -v rg >/dev/null 2>&1; then
    rg -n -i "$PATTERN" "$TARGET" --glob '*.java' || true
  else
    grep -rEni "$PATTERN" "$TARGET" --include='*.java' || true
  fi
}

violations=$(find_violations)

if [[ -n "$violations" ]]; then
  echo "Non-sargable date filter in analytics SQL:"
  echo "$violations"
  exit 1
fi

echo "Analytics SQL gate: OK (no year()/date_format() in WHERE)"
