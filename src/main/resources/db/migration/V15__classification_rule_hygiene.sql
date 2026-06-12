-- Rule engine data hygiene: deactivate invalid/orphan rules (no DELETE).

-- Blank patterns cannot match anything.
UPDATE cls_rule
SET active = 0,
    remark = CONCAT(COALESCE(remark, ''), ' [auto-disabled: blank pattern]')
WHERE (pattern IS NULL OR TRIM(pattern) = '')
  AND COALESCE(active, 1) = 1;

-- Orphan: category_id not found in active categories (soft-deleted or missing).
UPDATE cls_rule r
LEFT JOIN cls_category c ON c.code = r.category_id OR c.id = r.category_id
SET r.active = 0,
    r.remark = CONCAT(COALESCE(r.remark, ''), ' [auto-disabled: orphan category]')
WHERE r.category_id IS NOT NULL
  AND TRIM(r.category_id) <> ''
  AND (c.id IS NULL OR COALESCE(c.deleted, 0) = 1)
  AND COALESCE(r.active, 1) = 1;

-- Normalize category_id on rules to category code where possible.
UPDATE cls_rule r
INNER JOIN cls_category c ON c.id = r.category_id AND COALESCE(c.deleted, 0) = 0
SET r.category_id = c.code
WHERE r.category_id = c.id
  AND c.code IS NOT NULL
  AND TRIM(c.code) <> '';
