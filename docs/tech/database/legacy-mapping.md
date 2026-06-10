# Legacy → canonical mapping

| Legacy | Canonical | Notes |
|--------|-----------|-------|
| `app_user` | `fs_user` | Renamed (not `auth_user` — Django legacy table exists) |
| `consume_category` | `cls_category` | View `consume_category` removed after code switch |
| `transaction_temp` | `imp_staging_entry` | View `transaction_temp` for transition |
| `bank_card` | `fin_bank_account` | View `bank_card` |
| `RECORDID` / `recordID` | `statement_id` | On `transaction` / staging |
| `DEMOAREA` / `demoArea` | `memo` | On `transaction` |
| `CREATEUSER` / `createuser` | `created_by` | All migrated tables |
| `medical` / `endowment` / `accumulation` / `unemployment` | `ben_contribution` | `benefit_type` discriminator |
| `house_rent` rows | `transaction` | Migrated with `txn_kind=expense` |

**Not renamed:** table `transaction` (per product constraint).
