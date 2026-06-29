-- Add medical_spending semantic tag for healthcare categories.

UPDATE cls_category SET semantic_tag = 'medical_spending'
WHERE code = 'DAILY-05'
  AND coalesce(deleted, 0) = 0;

UPDATE cls_category SET semantic_tag = 'medical_spending'
WHERE coalesce(deleted, 0) = 0
  AND parent_id = 'LIVING'
  AND name REGEXP '医疗|医院|药|体检|挂号|牙科|疫苗'
  AND semantic_tag IN ('essential_spending', 'dining_spending', 'daily_spending', 'other_expense');

UPDATE cls_category SET semantic_tag = 'medical_spending'
WHERE coalesce(deleted, 0) = 0
  AND code LIKE 'LIVING-06%'
  AND (semantic_tag IS NULL OR semantic_tag = '' OR semantic_tag = 'essential_spending');
