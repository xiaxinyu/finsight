#!/usr/bin/env bash
# Repair failed Flyway migrations then re-apply pending scripts.
# Use after a migration failure (e.g. V19 syntax error) once the SQL file is fixed.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q flyway:repair flyway:migrate
echo "Flyway repair + migrate complete."
