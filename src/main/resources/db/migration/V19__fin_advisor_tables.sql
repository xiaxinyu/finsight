-- Advisor layer: planning persistence, metrics, profile, forecast, recommendations (additive only).

CREATE TABLE IF NOT EXISTS fin_budget (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(64)  NOT NULL,
    month_key       VARCHAR(16)  NOT NULL,
    name            VARCHAR(128) NULL,
    period_type     VARCHAR(32)  NOT NULL DEFAULT 'monthly',
    year_val        INT          NULL,
    month_val       INT          NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)  NULL,
    created_at      DATETIME(3)  NULL,
    updated_by      VARCHAR(64)  NULL,
    updated_at      DATETIME(3)  NULL,
    KEY idx_fin_budget_user_month (user_id, month_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_budget_line (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    budget_id       VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(64)  NOT NULL,
    category_code   VARCHAR(64)  NULL,
    bucket_key      VARCHAR(64)  NULL,
    limit_amount    DECIMAL(18,2) NULL,
    rollover        TINYINT(1)   NULL,
    created_by      VARCHAR(64)  NULL,
    created_at      DATETIME(3)  NULL,
    updated_by      VARCHAR(64)  NULL,
    updated_at      DATETIME(3)  NULL,
    KEY idx_fin_budget_line_budget (budget_id),
    KEY idx_fin_budget_line_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_bill (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    amount          DECIMAL(18,2) NULL,
    due_day         INT          NULL,
    recurrence      VARCHAR(32)  NULL,
    account_id      VARCHAR(64)  NULL,
    category_code   VARCHAR(64)  NULL,
    enabled         TINYINT(1)   NOT NULL DEFAULT 1,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    created_by      VARCHAR(64)  NULL,
    created_at      DATETIME(3)  NULL,
    updated_by      VARCHAR(64)  NULL,
    updated_at      DATETIME(3)  NULL,
    KEY idx_fin_bill_user (user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_financial_goal (
    id                      VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id                 VARCHAR(64)  NOT NULL,
    name                    VARCHAR(128) NOT NULL,
    goal_type               VARCHAR(32)  NULL,
    target_amount           DECIMAL(18,2) NULL,
    current_amount          DECIMAL(18,2) NULL,
    target_date             DATE         NULL,
    monthly_contribution    DECIMAL(18,2) NULL,
    linked_account_id       VARCHAR(64)  NULL,
    deleted                 TINYINT(1)   NOT NULL DEFAULT 0,
    created_by              VARCHAR(64)  NULL,
    created_at              DATETIME(3)  NULL,
    updated_by              VARCHAR(64)  NULL,
    updated_at              DATETIME(3)  NULL,
    KEY idx_fin_goal_user (user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_account_balance_snapshot (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(64)  NOT NULL,
    card_id         VARCHAR(64)  NOT NULL,
    balance         DECIMAL(18,2) NOT NULL,
    snapshot_date   DATE         NOT NULL,
    source          VARCHAR(32)  NULL,
    created_by      VARCHAR(64)  NULL,
    created_at      DATETIME(3)  NULL,
    KEY idx_fin_balance_snap_card (card_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_metric_monthly (
    user_id         VARCHAR(64)  NOT NULL,
    month_key       VARCHAR(7)   NOT NULL,
    metric_code     VARCHAR(64)  NOT NULL,
    metric_value    DECIMAL(18,4) NOT NULL,
    computed_at     DATETIME(3)  NOT NULL,
    PRIMARY KEY (user_id, month_key, metric_code),
    KEY idx_fin_metric_month (month_key, metric_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_profile_snapshot (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(64)  NOT NULL,
    snapshot_date   DATE         NOT NULL,
    dimension       VARCHAR(64)  NOT NULL,
    score           DECIMAL(6,2) NULL,
    level_label     VARCHAR(32)  NULL,
    payload_json    JSON         NULL,
    created_at      DATETIME(3)  NOT NULL,
    KEY idx_fin_profile_user_date (user_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_forecast_run (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(64)  NOT NULL,
    scenario        VARCHAR(32)  NOT NULL,
    target_year     INT          NOT NULL,
    params_json     JSON         NULL,
    created_at      DATETIME(3)  NOT NULL,
    KEY idx_fin_forecast_run_user (user_id, target_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_forecast_line (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    run_id          VARCHAR(64)  NOT NULL,
    month_key       VARCHAR(7)   NOT NULL,
    metric_code     VARCHAR(64)  NOT NULL,
    metric_value    DECIMAL(18,4) NOT NULL,
    lower_bound     DECIMAL(18,4) NULL,
    upper_bound     DECIMAL(18,4) NULL,
    KEY idx_fin_forecast_line_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_insight_card (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(64)  NOT NULL,
    card_type       VARCHAR(64)  NOT NULL,
    priority        INT          NOT NULL DEFAULT 50,
    title           VARCHAR(256) NOT NULL,
    reason          TEXT         NULL,
    impact_amount   DECIMAL(18,2) NULL,
    evidence_json   JSON         NULL,
    actions_json    JSON         NULL,
    created_at      DATETIME(3)  NOT NULL,
    expires_at      DATETIME(3)  NULL,
    KEY idx_fin_insight_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_recommendation_feedback (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(64)  NOT NULL,
    card_id         VARCHAR(64)  NOT NULL,
    action          VARCHAR(32)  NOT NULL,
    created_at      DATETIME(3)  NOT NULL,
    snooze_until    DATETIME(3)  NULL,
    KEY idx_fin_reco_feedback_user (user_id, card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fin_merchant_profile (
    id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(64)  NOT NULL,
    merchant_token  VARCHAR(128) NOT NULL,
    display_name    VARCHAR(256) NULL,
    is_subscription TINYINT(1)   NOT NULL DEFAULT 0,
    avg_amount      DECIMAL(18,2) NULL,
    txn_count       INT          NULL,
    last_seen       DATE         NULL,
    payload_json    JSON         NULL,
    updated_at      DATETIME(3)  NOT NULL,
    UNIQUE KEY uk_fin_merchant_user_token (user_id, merchant_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
