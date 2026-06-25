-- V35: Soft-delete for classification rules (align with cls_category / transaction).

ALTER TABLE cls_rule
    ADD COLUMN deleted INT NOT NULL DEFAULT 0 AFTER active;

CREATE INDEX idx_cls_rule_deleted ON cls_rule (deleted);
