#!/usr/bin/env bash
# Verify internal markdown links in docs/ resolve to existing files.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
fail=0
while IFS= read -r -d '' file; do
  while IFS= read -r link; do
    [[ -z "$link" ]] && continue
    link="${link%%#*}"
    link="${link%%\?*}"
    [[ "$link" =~ ^https?:// ]] && continue
    target="$ROOT/docs/${link#docs/}"
    if [[ ! -e "$target" ]]; then
      echo "BROKEN: $file -> $link"
      fail=1
    fi
  done < <(grep -oE '\]\([^)]+\)' "$file" | sed 's/]\(//;s/)$//' | grep -E '^(\./|\.\./|docs/)' || true)
done < <(find docs -name '*.md' -print0)
exit "$fail"
