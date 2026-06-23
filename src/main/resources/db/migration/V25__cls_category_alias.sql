-- V25: Category alias table for merge/traceability (schema only).

CREATE TABLE IF NOT EXISTS cls_category_alias (
    id              VARCHAR(32)  NOT NULL PRIMARY KEY,
    category_id     VARCHAR(32)  NOT NULL COMMENT 'Active cls_category.id or code ref',
    alias_code      VARCHAR(64)  NULL,
    alias_name      VARCHAR(128) NULL,
    reason          VARCHAR(256) NULL,
    created_by      VARCHAR(64)  NULL,
    created_at      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64)  NULL,
    updated_at      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_cls_category_alias_category (category_id),
    KEY idx_cls_category_alias_code (alias_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
