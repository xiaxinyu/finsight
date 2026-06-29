-- Pet categories mis-tagged as medical when name contains 医疗 (e.g. 宠物支出（食品、医疗）).

UPDATE cls_category SET semantic_tag = 'daily_spending'
WHERE coalesce(deleted, 0) = 0
  AND name LIKE '%宠物%'
  AND semantic_tag = 'medical_spending';

UPDATE cls_category SET semantic_tag = 'daily_spending'
WHERE coalesce(deleted, 0) = 0
  AND code IN ('DAILY-06', 'DAILY-07')
  AND (semantic_tag IS NULL OR semantic_tag = '' OR semantic_tag = 'dining_spending');
