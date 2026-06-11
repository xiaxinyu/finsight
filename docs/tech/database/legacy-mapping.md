# Legacy → canonical mapping

| Legacy | Canonical | Notes |
|--------|-----------|-------|
| `app_user` | `fs_user` | Renamed (not `auth_user` — Django legacy table exists) |
| `consume_category` | `cls_category` | Archived to `_archive_consume_category` when counts match |
| `consume_rule` | `cls_rule` | Archived to `_archive_consume_rule` when counts match |
| `transaction_temp` | `imp_staging_entry` | VIEW alias |
| `bank_card` | `fin_bank_account` | VIEW alias |
| `card` (table) | `fin_bank_account` | Table archived; VIEW `card` for legacy reads |
| `RECORDID` / `recordID` | `statement_id` | Java: prefer `statementId` |
| `DEMOAREA` / `demoArea` | `memo` | Java: prefer `memo` |
| `consume_id` / `consumeID` | `category_id` | Dual-write during transition |
| `consume_code` | `category_code` | Dual-write during transition |
| `consume_name` | `category_name` | Dual-write during transition |
| `balance_money` (expense) | `expense_amount` | Dual-write during transition |
| `CREATEUSER` / `createuser` | `created_by` | All migrated tables |
| `medical` / `endowment` / `accumulation` / `unemployment` | `ben_contribution` | `benefit_type` discriminator; XML mappers target `ben_contribution` |
| `house_rent` rows | `transaction` | Migrated with `txn_kind=expense` (historical) |

## API paths

| Legacy | Canonical |
|--------|-----------|
| `/api/v1/consume/categories` | `/api/v1/classification/categories` |
| `/api/v1/consume/rules` | `/api/v1/classification/rules` |

**Not renamed:** table `transaction` (per product constraint).
