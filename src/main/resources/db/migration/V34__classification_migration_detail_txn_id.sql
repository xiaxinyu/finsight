-- V34: transaction.id is UUID (36) or hex (32); detail audit column was VARCHAR(32) and truncated.

ALTER TABLE classification_migration_detail
    MODIFY COLUMN transaction_id VARCHAR(64) NOT NULL;
