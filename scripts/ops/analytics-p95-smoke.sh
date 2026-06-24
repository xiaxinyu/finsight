#!/usr/bin/env bash
# v2.0.0 GA smoke: measure warm-cache P95 for Profile / Advisor / Forecast APIs.
#
# Usage:
#   export FINSIGHT_BASE_URL=https://your-host
#   export FINSIGHT_USER=admin
#   export FINSIGHT_PASS='secret'
#   bash scripts/ops/analytics-p95-smoke.sh
#
# Optional:
#   FINSIGHT_ROUNDS=20        sample count per endpoint (default 20)
#   FINSIGHT_WARMUP=3         discarded warmup requests (default 3)
#   FINSIGHT_PROFILE_BUDGET_MS=800
#   FINSIGHT_ADVISOR_BUDGET_MS=500
#   FINSIGHT_FORECAST_BUDGET_MS=1000
#   FINSIGHT_TREND_BUDGET_MS=1200
#   FINSIGHT_CASH_RISK_BUDGET_MS=1200
#   FINSIGHT_COOKIE_JAR=/tmp/finsight-smoke-cookies.txt
set -euo pipefail

BASE_URL="${FINSIGHT_BASE_URL:-http://127.0.0.1:8080}"
USER="${FINSIGHT_USER:-}"
PASS="${FINSIGHT_PASS:-}"
ROUNDS="${FINSIGHT_ROUNDS:-20}"
WARMUP="${FINSIGHT_WARMUP:-3}"
PROFILE_BUDGET_MS="${FINSIGHT_PROFILE_BUDGET_MS:-800}"
ADVISOR_BUDGET_MS="${FINSIGHT_ADVISOR_BUDGET_MS:-500}"
FORECAST_BUDGET_MS="${FINSIGHT_FORECAST_BUDGET_MS:-1000}"
TREND_BUDGET_MS="${FINSIGHT_TREND_BUDGET_MS:-1200}"
CASH_RISK_BUDGET_MS="${FINSIGHT_CASH_RISK_BUDGET_MS:-1200}"
YEAR=$(date +%Y)
COOKIE_JAR="${FINSIGHT_COOKIE_JAR:-/tmp/finsight-smoke-cookies.txt}"

if [[ -z "$USER" || -z "$PASS" ]]; then
  echo "Set FINSIGHT_USER and FINSIGHT_PASS for form login."
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required."
  exit 1
fi

rm -f "$COOKIE_JAR"

login_status=$(curl -sS -o /dev/null -w "%{http_code}" \
  -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "${BASE_URL}/authentication/form" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "username=${USER}" \
  --data-urlencode "password=${PASS}")

if [[ "$login_status" != "302" && "$login_status" != "200" ]]; then
  echo "Login failed (HTTP ${login_status}). Check BASE_URL and credentials."
  exit 1
fi

measure_endpoint() {
  local name="$1"
  local path="$2"
  local budget_ms="$3"
  local -a samples=()
  local total=$((WARMUP + ROUNDS))
  local i

  for ((i = 1; i <= total; i++)); do
    local ms
    ms=$(curl -sS -o /dev/null -w "%{time_total}" \
      -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
      "${BASE_URL}${path}" | awk '{printf "%.0f", $1 * 1000}')
    if (( i > WARMUP )); then
      samples+=("$ms")
    fi
  done

  local sorted
  sorted=$(printf '%s\n' "${samples[@]}" | sort -n)
  local count=${#samples[@]}
  local p50_idx=$(( (count * 50 + 99) / 100 ))
  local p95_idx=$(( (count * 95 + 99) / 100 ))
  local p50 p95 max
  p50=$(echo "$sorted" | sed -n "${p50_idx}p")
  p95=$(echo "$sorted" | sed -n "${p95_idx}p")
  max=$(echo "$sorted" | tail -1)

  printf '%s p50=%sms p95=%sms max=%sms budget=%sms samples=%s\n' \
    "$name" "$p50" "$p95" "$max" "$budget_ms" "$count"

  if (( p95 > budget_ms )); then
    echo "FAIL: ${name} warm P95 ${p95}ms exceeds budget ${budget_ms}ms"
    return 1
  fi
  return 0
}

echo "FinSight analytics P95 smoke"
echo "  base=${BASE_URL}"
echo "  rounds=${ROUNDS} warmup=${WARMUP}"
echo

failed=0
measure_endpoint "profile" "/api/v1/analytics/profile" "$PROFILE_BUDGET_MS" || failed=1
measure_endpoint "advisor" "/api/v1/advisor/recommendations" "$ADVISOR_BUDGET_MS" || failed=1
measure_endpoint "forecast" "/api/v1/analytics/forecast?months=6" "$FORECAST_BUDGET_MS" || failed=1
measure_endpoint "trends" "/api/v1/analytics/trends?fromYear=$((YEAR-1))&toYear=${YEAR}" "$TREND_BUDGET_MS" || failed=1
measure_endpoint "cash-risk" "/api/v1/analytics/cash-risk-calendar?year=${YEAR}&scenario=stress" "$CASH_RISK_BUDGET_MS" || failed=1

if (( failed != 0 )); then
  exit 1
fi

echo
echo "PASS: all endpoints within warm-cache P95 budgets."
