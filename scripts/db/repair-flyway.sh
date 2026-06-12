#!/usr/bin/env bash
# Remove failed Flyway history rows so the app can restart and retry migrations.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
VERSION="${1:-}"
mvn -q dependency:build-classpath -Dmdep.includeScope=runtime -Dmdep.outputFile=tmp/cp.txt
javac -cp "$(cat tmp/cp.txt)" scripts/db/FlywayRepairFailed.java -d tmp/
if [[ -n "$VERSION" ]]; then
  java -cp "tmp:$(cat tmp/cp.txt)" FlywayRepairFailed "$VERSION"
else
  java -cp "tmp:$(cat tmp/cp.txt)" FlywayRepairFailed
fi
echo "Done. Restart: mvn spring-boot:run"
