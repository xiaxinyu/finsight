# Historical Flyway scripts (V1–V10)

Versions 1–10 were applied to existing databases before migration SQL was committed to this repository.
See `flyway_schema_history` for applied versions.

New environments: `spring.flyway.baseline-version=10` then **V11+** in `../` bootstraps core schema.

Do not place V1–V10 files in the active migration folder unless checksums are repaired with `flyway repair`.
