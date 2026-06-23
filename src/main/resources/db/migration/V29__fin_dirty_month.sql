-- V29: Dirty month markers for metric refresh (schema only).

CREATE TABLE IF NOT EXISTS fin_dirty_month (
    month_key   VARCHAR(7)   NOT NULL PRIMARY KEY COMMENT 'yyyy-MM',
    marked_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refreshed_at DATETIME    NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
