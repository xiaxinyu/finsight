-- Optional: add transfer markers to existing transaction table (not new tables).
-- Run manually if you use "Mark as transfer" in Transactions UI.
-- Safe to re-run: ignore "Duplicate column" errors if columns already exist.

USE finsight;

ALTER TABLE transaction ADD COLUMN txn_kind VARCHAR(32) NULL;
ALTER TABLE transaction ADD COLUMN transfer_group_id VARCHAR(64) NULL;
