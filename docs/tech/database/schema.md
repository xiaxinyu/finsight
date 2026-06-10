# FinSight Database Schema

Schema changes are versioned in `src/main/resources/db/migration/` (Flyway).

## Naming rules

- Tables: `snake_case` with domain prefix where applicable (`auth_`, `cls_`, `fin_`, `imp_`, `ben_`)
- Columns: `snake_case` only (`created_at`, `statement_id`, `memo`)
- Java entities: `camelCase` fields; MyBatis-Plus `map-underscore-to-camel-case: true`
- **Ledger**: table name stays `transaction` (single source of truth for money rows)

## Active tables (application-managed)

| Table | Domain | Purpose |
|-------|--------|---------|
| `transaction` | fin | Committed ledger entries |
| `imp_staging_entry` | imp | Import preview (`transaction_temp` view alias) |
| `statement` | imp | Uploaded statement files |
| `fin_bank_account` | fin | Bank cards (`bank_card` view alias) |
| `cls_category` | cls | Category tree |
| `cls_rule` / `cls_rule_tag` | cls | Classification rules |
| `fs_user` / `fs_role` / `fs_user_role` | auth | Login & RBAC (`fs_` avoids Django `auth_user` clash) |
| `ben_contribution` | ben | Social insurance contributions (4 legacy types) |
| `card` | legacy | Legacy card dimension (deprecated; mapped via migration) |

## Out of scope (not migrated)

Unused by runtime code: `transfer_pair`, `transaction_link`, `budget*`, `bill`, `financial_goal`, `account_balance_snapshot`, Django `auth_*` tables, `CREDIT`, `deposit*`, `salary`.

## Audit columns

All new/updated tables use: `created_at`, `updated_at`, `created_by`, `updated_by`, `version`, `deleted` (where applicable).
