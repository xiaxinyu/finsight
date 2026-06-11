# Schema & rule engine regression checklist

Unused-table cleanup is **manual** only: `scripts/db/cleanup-unused-tables.sql` (not on app startup).

After Flyway migrate:

1. `mvn clean package`
2. `mvn checkstyle:check`
3. `POST /api/v1/maintenance/verify-schema-migration` — `ok: true`, review `orphanRuleRows`
4. Statement import: Parsed / Skipped tabs
5. Admin: Categories, Rule Engine CRUD
6. `POST /api/v1/classification/rules/test` with sample narration
7. Transactions list / filters
8. Benefits + House Rent API smoke
9. Planning / Goals (in-memory)

Production deploy order: `flyway migrate` → verify endpoint → smoke UI.
