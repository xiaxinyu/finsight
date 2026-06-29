-- Restore categories accidentally hidden when update wrote deleted = NULL.
UPDATE cls_category SET deleted = 0 WHERE deleted IS NULL;
