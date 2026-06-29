-- Persist admin-selected reporting classification (semantic tag id).
ALTER TABLE cls_category
    ADD COLUMN semantic_tag VARCHAR(32) NULL DEFAULT NULL AFTER report_role;
