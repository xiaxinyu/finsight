-- Normalize all existing ledger / planning data to owner xiaxinyu (single-user bootstrap).
-- Skips tables that are not present in this database (partial advisor schema installs).

DROP PROCEDURE IF EXISTS finsight_backfill_owner_xiaxinyu;
DELIMITER $$
CREATE PROCEDURE finsight_backfill_owner_xiaxinyu()
BEGIN
    DECLARE p_owner VARCHAR(64) DEFAULT 'xiaxinyu';

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction') THEN
        UPDATE `transaction`
        SET created_by = p_owner, updated_by = p_owner
        WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'statement') THEN
        UPDATE `statement`
        SET created_by = p_owner, updated_by = p_owner
        WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'imp_staging_entry') THEN
        UPDATE `imp_staging_entry`
        SET created_by = p_owner, updated_by = p_owner
        WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_bank_account') THEN
        UPDATE `fin_bank_account`
        SET created_by = p_owner, updated_by = p_owner
        WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ben_contribution') THEN
        UPDATE `ben_contribution`
        SET created_by = p_owner, updated_by = p_owner
        WHERE created_by IS NULL OR TRIM(created_by) = '' OR created_by <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_metric_monthly') THEN
        UPDATE `fin_metric_monthly` SET user_id = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_profile_current') THEN
        UPDATE `fin_profile_current` SET user_id = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_profile_snapshot') THEN
        UPDATE `fin_profile_snapshot` SET user_id = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_budget') THEN
        UPDATE `fin_budget` SET user_id = p_owner, created_by = p_owner, updated_by = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_budget_line') THEN
        UPDATE `fin_budget_line` SET user_id = p_owner, created_by = p_owner, updated_by = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_bill') THEN
        UPDATE `fin_bill` SET user_id = p_owner, created_by = p_owner, updated_by = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_goal') THEN
        UPDATE `fin_goal` SET user_id = p_owner, created_by = p_owner, updated_by = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_forecast_run') THEN
        UPDATE `fin_forecast_run` SET user_id = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_advisor_insight') THEN
        UPDATE `fin_advisor_insight` SET user_id = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_reco_feedback') THEN
        UPDATE `fin_reco_feedback` SET user_id = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fin_merchant') THEN
        UPDATE `fin_merchant` SET user_id = p_owner
        WHERE user_id IS NULL OR TRIM(user_id) = '' OR user_id <> p_owner;
    END IF;
END$$
DELIMITER ;

CALL finsight_backfill_owner_xiaxinyu();
DROP PROCEDURE finsight_backfill_owner_xiaxinyu;
