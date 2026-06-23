-- V26: Batch audit for classification changes (schema only).

CREATE TABLE IF NOT EXISTS classification_migration_batch (
    id              VARCHAR(32)  NOT NULL PRIMARY KEY,
    batch_type      VARCHAR(32)  NOT NULL COMMENT 'RECLASSIFY, RULE_APPLY, etc.',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PREVIEW' COMMENT 'PREVIEW, APPLIED, FAILED',
    reason          VARCHAR(512) NULL,
    row_count       INT          NULL DEFAULT 0,
    created_by      VARCHAR(64)  NULL,
    created_at      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64)  NULL,
    updated_at      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    applied_at      DATETIME     NULL,
    KEY idx_cls_mig_batch_status (status),
    KEY idx_cls_mig_batch_type (batch_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS classification_migration_detail (
    id                  VARCHAR(32)  NOT NULL PRIMARY KEY,
    batch_id            VARCHAR(32)  NOT NULL,
    transaction_id      VARCHAR(32)  NOT NULL,
    old_consume_code    VARCHAR(64)  NULL,
    new_consume_code    VARCHAR(64)  NULL,
    old_consume_name    VARCHAR(128) NULL,
    new_consume_name    VARCHAR(128) NULL,
    action              VARCHAR(32)  NULL,
    rule_id             VARCHAR(32)  NULL,
    created_at          DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_cls_mig_detail_batch (batch_id),
    KEY idx_cls_mig_detail_txn (transaction_id),
    CONSTRAINT fk_cls_mig_detail_batch FOREIGN KEY (batch_id)
        REFERENCES classification_migration_batch (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
