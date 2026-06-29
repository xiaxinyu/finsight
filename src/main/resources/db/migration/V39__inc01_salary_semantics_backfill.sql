-- Backfill salary category semantics after admin classification rollout.
UPDATE cls_category
SET report_role = 'income',
    semantic_tag = 'real_income'
WHERE code = 'INC-01'
  AND coalesce(deleted, 0) = 0
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '' OR report_role IS NULL OR report_role = 'budget');
