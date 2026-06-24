#!/usr/bin/env bash
# Verify internal markdown links in docs/ resolve to existing files.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
fail=0
while IFS= read -r -d '' file; do
  base_dir="$(cd "$(dirname "$file")" && pwd)"
  while IFS= read -r raw; do
    link="${raw#](}"
    link="${link%)}"
    [[ -z "$link" ]] && continue
    link="${link%%#*}"
    link="${link%%\?*}"
    [[ "$link" =~ ^https?:// ]] && continue
    if [[ "$link" == docs/* ]]; then
      target="$ROOT/$link"
    else
      target="$base_dir/$link"
    fi
    if [[ ! -e "$target" ]]; then
      echo "BROKEN: $file -> $link"
      fail=1
    fi
  done < <(grep -oE '\]\([^)]+\)' "$file" | grep -E '^\]\((\./|\.\./|docs/)' || true)
done < <(find docs -name '*.md' -print0)
exit "$fail"
