-- Unify fixed_spending into flat fixed tags; fix LIVING over-assignment to dining_spending.

UPDATE cls_category SET semantic_tag = 'fixed_housing'
WHERE code = 'FIXED-01' AND (semantic_tag = 'fixed_spending' OR semantic_tag IS NULL OR semantic_tag = '');

UPDATE cls_category SET semantic_tag = 'fixed_utilities'
WHERE code = 'FIXED-02' AND (semantic_tag = 'fixed_spending' OR semantic_tag IS NULL OR semantic_tag = '');

UPDATE cls_category SET semantic_tag = 'fixed_telecom'
WHERE code = 'FIXED-03' AND (semantic_tag = 'fixed_spending' OR semantic_tag IS NULL OR semantic_tag = '');

UPDATE cls_category SET semantic_tag = 'fixed_insurance'
WHERE code = 'FIXED-04' AND (semantic_tag IN ('fixed_spending', 'essential_spending') OR semantic_tag IS NULL OR semantic_tag = '');

UPDATE cls_category SET semantic_tag = 'fixed_tuition'
WHERE code = 'FIXED-06' AND (semantic_tag = 'fixed_spending' OR semantic_tag IS NULL OR semantic_tag = '');

UPDATE cls_category SET semantic_tag = 'fixed_repayment'
WHERE code = 'FIXED-07' AND (semantic_tag = 'fixed_spending' OR semantic_tag IS NULL OR semantic_tag = '');

UPDATE cls_category SET semantic_tag = 'fixed_misc'
WHERE code = 'FIXED-99' AND (semantic_tag = 'fixed_spending' OR semantic_tag IS NULL OR semantic_tag = '');

UPDATE cls_category SET semantic_tag = 'fixed_housing'
WHERE code = 'FIXED' AND level = 1 AND (semantic_tag = 'fixed_spending' OR semantic_tag IS NULL OR semantic_tag = '');

UPDATE cls_category SET semantic_tag = 'daily_spending'
WHERE parent_id = 'LIVING' AND semantic_tag = 'dining_spending'
  AND name NOT REGEXP '餐饮|外卖|堂食|早餐|咖啡|饭店|吃饭|小吃';

UPDATE cls_category SET semantic_tag = 'essential_spending'
WHERE parent_id = 'LIVING' AND semantic_tag = 'dining_spending'
  AND name REGEXP '医疗|医院|药|体检|挂号';

UPDATE cls_category SET semantic_tag = 'daily_spending'
WHERE parent_id = 'LIVING' AND semantic_tag = 'dining_spending'
  AND name REGEXP '宠物|家政|快递|保洁|维修';

UPDATE cls_category SET semantic_tag = 'daily_spending'
WHERE code = 'LIVING' AND level = 1 AND semantic_tag = 'dining_spending';

UPDATE cls_category c
JOIN cls_category p ON c.parent_id = p.code
SET c.semantic_tag = CASE c.code
    WHEN 'FIXED-01' THEN 'fixed_housing'
    WHEN 'FIXED-02' THEN 'fixed_utilities'
    WHEN 'FIXED-03' THEN 'fixed_telecom'
    WHEN 'FIXED-04' THEN 'fixed_insurance'
    WHEN 'FIXED-06' THEN 'fixed_tuition'
    WHEN 'FIXED-07' THEN 'fixed_repayment'
    WHEN 'FIXED-99' THEN 'fixed_misc'
    ELSE 'fixed_misc'
END
WHERE c.semantic_tag = 'fixed_spending'
  AND (p.code = 'FIXED' OR c.code LIKE 'FIXED-%');
