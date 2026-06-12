#!/usr/bin/env bash
# Apply FinSight personal finance tables (manual DDL — not run by the app).
#
# Prerequisites: MySQL client (`mysql`) on PATH; database `finsight` already exists.
#
# Defaults match src/main/resources/application.yml. Override via env:
#   MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD MYSQL_DATABASE
#   or SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
#
# Examples:
#   ./scripts/db/apply-personal-finance-schema.sh
#   MYSQL_PASSWORD=secret ./scripts/db/apply-personal-finance-schema.sh
#   mysql -u root -p finsight < scripts/db/V1_personal_finance.sql

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SQL_FILE="${ROOT}/scripts/db/V1_personal_finance.sql"

HOST="${MYSQL_HOST:-127.0.0.1}"
PORT="${MYSQL_PORT:-3306}"
USER="${SPRING_DATASOURCE_USERNAME:-${MYSQL_USER:-root}}"
PASS="${SPRING_DATASOURCE_PASSWORD:-${MYSQL_PASSWORD:-123456}}"
DB="${MYSQL_DATABASE:-finsight}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "SQL file not found: $SQL_FILE" >&2
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql client not found. Run manually:" >&2
  echo "  mysql -h $HOST -P $PORT -u $USER -p $DB < $SQL_FILE" >&2
  exit 1
fi

echo "Applying schema to ${DB}@${HOST}:${PORT} as ${USER} ..."
mysql -h"$HOST" -P"$PORT" -u"$USER" -p"$PASS" "$DB" < "$SQL_FILE"
echo "Done."
