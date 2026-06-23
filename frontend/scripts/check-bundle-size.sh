#!/usr/bin/env bash
# Fail if main JS bundle exceeds budget (v2.0.0 gate).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ASSETS="$ROOT/src/main/resources/static/app/assets"
MAX_BYTES="${FINSIGHT_BUNDLE_MAX_BYTES:-4200000}"

cd "$ROOT/frontend"
npm run build --silent

MAIN=$(find "$ASSETS" -name 'index-*.js' -type f | head -1)
if [[ -z "$MAIN" ]]; then
  echo "Bundle check failed: index-*.js not found under $ASSETS"
  exit 1
fi

SIZE=$(stat -f%z "$MAIN" 2>/dev/null || stat -c%s "$MAIN")
echo "Main bundle: $MAIN ($SIZE bytes, budget $MAX_BYTES)"

if [[ "$SIZE" -gt "$MAX_BYTES" ]]; then
  echo "Bundle size exceeded budget by $((SIZE - MAX_BYTES)) bytes"
  exit 1
fi
