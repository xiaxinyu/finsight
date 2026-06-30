-- P1/P2: Fee, Groceries, Tax semantic tags; map existing categories.

UPDATE cls_category
SET semantic_tag = 'finance_fee'
WHERE (parent_id IN ('FEE', 'FE') OR code LIKE 'FEE-%')
  AND COALESCE(deleted, 0) = 0
  AND (semantic_tag IS NULL OR semantic_tag IN ('essential_spending', 'other', 'daily_spending'));

UPDATE cls_category
SET semantic_tag = 'groceries_spending'
WHERE code IN ('DAILY-03', 'DAILY-04')
  AND COALESCE(deleted, 0) = 0;

UPDATE cls_category
SET semantic_tag = 'groceries_spending'
WHERE COALESCE(deleted, 0) = 0
  AND semantic_tag = 'shopping_spending'
  AND (name LIKE '%超市%' OR name LIKE '%食材%' OR name LIKE '%粮油%');

UPDATE cls_category
SET semantic_tag = 'tax_expense'
WHERE COALESCE(deleted, 0) = 0
  AND (name LIKE '%个税%' OR name LIKE '%所得税%' OR name LIKE '%物业税%' OR name LIKE '%房产税%')
  AND txn_types LIKE '%expense%'
  AND (semantic_tag IS NULL OR semantic_tag IN ('essential_spending', 'other_expense', 'daily_spending'));

UPDATE cls_category
SET semantic_tag = 'tax_refund'
WHERE COALESCE(deleted, 0) = 0
  AND (name LIKE '%退税%' OR name LIKE '%税返%')
  AND txn_types LIKE '%income%'
  AND (semantic_tag IS NULL OR semantic_tag IN ('other_income', 'real_income'));
