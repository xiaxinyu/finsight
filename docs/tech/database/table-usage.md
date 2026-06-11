# Table usage inventory

## Core (keep)

| Table | Entity | Purpose |
|-------|--------|---------|
| `transaction` | `Transaction` | Ledger |
| `imp_staging_entry` | `TransactionTemp` | Import staging (`transaction_temp` view) |
| `statement` | `Statement` | Uploaded files |
| `fin_bank_account` | `BankCard` | Bank cards (`bank_card` view) |
| `cls_category` | `Category` | Categories |
| `cls_rule` / `cls_rule_tag` | `ClassificationRule` | Rules |
| `fs_user` / `fs_role` / `fs_user_role` | `User` / `Role` | Auth |
| `ben_contribution` | `BenefitContribution`, benefit POJOs | Social insurance |
| `house_rent` | `HouseRent` | Rent API (legacy) |
| `flyway_schema_history` | — | Migrations |

## 可手动删除（见 `scripts/db/cleanup-unused-tables.sql`）

`_deprecated_*`, Django `auth_*` / `django_*`, `deposit*`, `CREDIT`, `card` view, `_archive_*` 等 — **不会在启动时自动删**，先跑脚本 STEP 1 盘点再决定。

## In-memory only

`Budget`, `BudgetLine`, `Bill`, `FinancialGoal` — `PlanningPreferencesStore`.
