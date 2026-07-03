-- One balance per user/card/day; enables upsert from statement imports.
ALTER TABLE fin_account_balance_snapshot
    ADD UNIQUE KEY uk_fin_balance_snap_user_card_date (user_id, card_id, snapshot_date);
