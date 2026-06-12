#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
mvn -q dependency:build-classpath -Dmdep.includeScope=runtime -Dmdep.outputFile=tmp/cp.txt
javac -proc:none -cp "$(cat tmp/cp.txt)" scripts/db/DropUnusedTables.java -d tmp/
java -cp "tmp:$(cat tmp/cp.txt)" DropUnusedTables
