-- Remove duplicate profile snapshots before adding uniqueness.
DELETE s1 FROM fin_profile_snapshot s1
INNER JOIN fin_profile_snapshot s2
    ON s1.user_id = s2.user_id
   AND s1.snapshot_date = s2.snapshot_date
   AND s1.dimension = s2.dimension
   AND (s1.created_at < s2.created_at
        OR (s1.created_at = s2.created_at AND s1.id < s2.id));

ALTER TABLE fin_profile_snapshot
    ADD UNIQUE KEY uk_fin_profile_snapshot_user_date_dim (user_id, snapshot_date, dimension);
