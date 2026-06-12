-- Drop unused application tables (no Java write paths). Legacy Django tables untouched.

DROP VIEW IF EXISTS v_legacy_house_rent;

-- Unused transfer models (txn_kind on transaction is canonical)
DROP TABLE IF EXISTS transfer_pair;
DROP TABLE IF EXISTS transaction_link;

-- Planning tables not used (in-memory PlanningPreferencesStore)
DROP TABLE IF EXISTS budget_line;
DROP TABLE IF EXISTS budget;
DROP TABLE IF EXISTS bill;
DROP TABLE IF EXISTS financial_goal;
DROP TABLE IF EXISTS account_balance_snapshot;
