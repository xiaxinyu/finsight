# Flyway migrations

SQL migrations live in `src/main/resources/db/migration/`. Flyway runs on application startup (`spring.flyway.enabled: true`).

## CI validation

GitHub Actions job **`backend-flyway`** runs `FlywayMigrationIntegrationTest` against a fresh MySQL 8 Testcontainers database. Any new migration script must pass this job before merge.

Locally (requires Docker):

```bash
mvn test -Dtest=FlywayMigrationIntegrationTest
```

Without Docker, the test is skipped automatically (`@Testcontainers(disabledWithoutDocker = true)`).

## Baseline

Existing databases created before Flyway use `baseline-version: 10` in `application.yml` / `pom.xml`. Fresh installs apply **V0–V20** in order (`V5_1` bootstraps legacy `statement` before `V6` normalizes it).

`out-of-order: true` allows `V5_1` to be recorded on databases already past V6 (e.g. at V20). The script is idempotent (`CREATE TABLE IF NOT EXISTS`); existing `statement` tables are unchanged.

Never edit an already-applied `Vn__*.sql`; add a new version (e.g. `V21__...`) instead.

## Checksum mismatch after `git pull`

If startup fails with `Migration checksum mismatch for migration version N`, a migration file changed after it was already applied locally. **Schema data is unchanged** — update Flyway metadata only:

```bash
mvn flyway:repair
# then restart the app (or: mvn flyway:migrate)
```

Do not edit migration files that are already in production without a repair plan; prefer adding a new `V21__...sql` for forward changes.

## Recovery (local)

If migration fails mid-way or `flyway_schema_history` is inconsistent:

```bash
# Inspect status (uses pom.xml flyway plugin defaults)
mvn flyway:info

# Repair checksum / failed migration metadata (no schema DDL)
mvn flyway:repair

# Apply pending migrations
mvn flyway:migrate
```

Typical order: **`mvn flyway:repair flyway:migrate`**.

Set connection via env or edit `pom.xml` flyway plugin only for local dev — do not commit real credentials.

## Runtime verification

`POST /api/v1/maintenance/verify-schema-migration` checks core tables, row counts, and orphan rules after deploy. See [schema.md](schema.md).
