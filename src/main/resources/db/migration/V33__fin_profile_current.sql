-- v2.0.2: materialized current profile (one row per user).
CREATE TABLE IF NOT EXISTS fin_profile_current (
    user_id             VARCHAR(64)   NOT NULL PRIMARY KEY,
    as_of_date          DATE          NOT NULL,
    overall_score       DECIMAL(6,2)  NULL,
    confidence          VARCHAR(16)   NULL,
    sample_months       INT           NULL,
    payload_json        JSON          NOT NULL,
    stale               TINYINT(1)    NOT NULL DEFAULT 0,
    computed_at         DATETIME(3)   NOT NULL,
    compute_duration_ms INT           NULL,
    error_message       VARCHAR(512)  NULL,
    profile_version     VARCHAR(32)   NOT NULL DEFAULT 'v2.0.2',
    KEY idx_fin_profile_current_stale (stale, computed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
