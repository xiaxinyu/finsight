# Table usage inventory

Generated from application code audit. **Do not DROP** tables marked *legacy-keep* without explicit approval.

## Core (application-managed)

| Table | Java entity | Purpose |
|-------|-------------|---------|
| `transaction` | `Transaction` | Committed ledger |
| `imp_staging_entry` | `TransactionTemp` | Import staging (`transaction_temp` view) |
| `statement` | `Statement` | Uploaded statement files |
| `fin_bank_account` | `BankCard` | Bank cards (`bank_card` view) |
| `cls_category` | `Category` / `ConsumeCategory` | Category tree |
| `cls_rule` | `ClassificationRule` / `ConsumeRule` | Classification rules |
| `cls_rule_tag` | `ClassificationRuleTag` / `ConsumeRuleTag` | Rule tags |
| `fs_user`, `fs_role`, `fs_user_role` | `User`, `Role` | Auth |
| `ben_contribution` | `BenefitContribution` + benefit XML mappers | Social insurance |

## Legacy-keep (API retained)

| Table | Notes |
|-------|-------|
| `house_rent` | `HouseRentController` |
| `_deprecated_medical`, `_deprecated_endowment`, etc. | Archived copies; runtime writes `ben_contribution` |
| `_archive_card_legacy` | After V16 migration; old `card` rows preserved |

## Archived by migration (data preserved, renamed)

| Archive table | Source |
|---------------|--------|
| `_archive_consume_category` | Duplicate of `cls_category` |
| `_archive_consume_rule` | Duplicate of `cls_rule` |
| `_archive_consume_rule_tag` | Duplicate of `cls_rule_tag` |

## In-memory only (no DB table required)

`Budget`, `BudgetLine`, `Bill`, `FinancialGoal` — stored in `PlanningPreferencesStore`.

## Dead code removed (no runtime mapper injection)

`transfer_pair`, `transaction_link`, `budget`, `budget_line`, `bill`, `financial_goal`, `financial_account`, `account_balance_snapshot`

## Out of scope (Django / historical)

`auth_*`, `django_*`, `deposit*`, `CREDIT`, `salary` — not managed by FinSight Flyway.
