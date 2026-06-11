# FinSight Database Schema

Schema changes are versioned in `src/main/resources/db/migration/` (Flyway).  
Existing databases at Flyway **≤ V10** use `baseline-version: 10`; **V11+** applies forward migrations only.

See also: [table-usage.md](table-usage.md), [legacy-mapping.md](legacy-mapping.md).

## Naming checklist (executable)

### Tables
- `snake_case` + domain prefix: `cls_`, `fin_`, `imp_`, `ben_`, `fs_`
- Exception: ledger table stays **`transaction`**

### Columns
- PK: `id`
- FK: `{entity}_id` (e.g. `category_id`, `statement_id`, `bank_card_id`)
- Money: `income_money`, `expense_amount` (transition from `balance_money` for expenses)
- Category on ledger: `category_id`, `category_code`, `category_name` (transition from `consume_*`)
- Audit: `created_at`, `updated_at`, `created_by`, `updated_by`, `version`, `deleted`

### Java
- Entities: `Category`, `ClassificationRule`, `Transaction`, `BankCard`
- Deprecated aliases: `ConsumeCategory`, `ConsumeRule`
- API: `/api/v1/classification/{categories,rules}`

## Active tables (application-managed)

| Table | Domain | Purpose |
|-------|--------|---------|
| `transaction` | fin | Committed ledger entries |
| `imp_staging_entry` | imp | Import preview (`transaction_temp` view) |
| `statement` | imp | Uploaded statement files |
| `fin_bank_account` | fin | Bank cards (`bank_card` view) |
| `cls_category` | cls | Category tree |
| `cls_rule` / `cls_rule_tag` | cls | Classification rules |
| `fs_user` / `fs_role` / `fs_user_role` | auth | Login & RBAC |
| `ben_contribution` | ben | Social insurance (legacy APIs write here) |
| `house_rent` | legacy | Rent listing API |
| `card` | legacy | VIEW over `fin_bank_account` after V16 |

## Archived (renamed, not dropped)

| Archive | Notes |
|---------|-------|
| `_archive_consume_category` | Duplicate of `cls_category` |
| `_archive_consume_rule` | Duplicate of `cls_rule` |
| `_archive_card_legacy` | Old `card` table |

## Out of scope (Django / historical)

`auth_*`, `django_*`, `deposit*`, `CREDIT`, `salary` — not managed by FinSight Flyway.

## Verification

`POST /api/v1/maintenance/verify-schema-migration` — row counts, orphan rules, missing core tables.
