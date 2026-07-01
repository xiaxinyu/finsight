-- Normalize all existing ledger / planning data to owner xiaxinyu (single-user bootstrap).
-- Does not modify fs_user / fs_role / cls_* system tables.

SET @owner := 'xiaxinyu';

UPDATE `transaction`
SET created_by = @owner, updated_by = @owner
WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> @owner;

UPDATE `statement`
SET created_by = @owner, updated_by = @owner
WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> @owner;

UPDATE `imp_staging_entry`
SET created_by = @owner, updated_by = @owner
WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> @owner;

UPDATE `fin_bank_account`
SET created_by = @owner, updated_by = @owner
WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> @owner;

UPDATE `ben_contribution`
SET created_by = @owner, updated_by = @owner
WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> @owner;

UPDATE `fin_metric_monthly` SET user_id = @owner WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_profile_current` SET user_id = @owner WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_profile_snapshot` SET user_id = @owner WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_budget` SET user_id = @owner, created_by = @owner, updated_by = @owner
WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_budget_line` SET user_id = @owner, created_by = @owner, updated_by = @owner
WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_bill` SET user_id = @owner, created_by = @owner, updated_by = @owner
WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_goal` SET user_id = @owner, created_by = @owner, updated_by = @owner
WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_forecast_run` SET user_id = @owner
WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_advisor_insight` SET user_id = @owner
WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_reco_feedback` SET user_id = @owner
WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
UPDATE `fin_merchant` SET user_id = @owner
WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> @owner;
