-- Default reporting classification (semantic_tag) for all active categories.
-- Preserves user overrides: only fills NULL/blank semantic_tag and report_role.

-- 1) report_role where missing
UPDATE cls_category SET report_role = 'income'
WHERE coalesce(deleted, 0) = 0 AND code IN ('INC', 'INCOME')
  AND (report_role IS NULL OR trim(report_role) = '');

UPDATE cls_category SET report_role = 'refund'
WHERE coalesce(deleted, 0) = 0 AND code IN ('REIM', 'REIMB')
  AND (report_role IS NULL OR trim(report_role) = '');

UPDATE cls_category SET report_role = 'asset'
WHERE coalesce(deleted, 0) = 0 AND code = 'ASSET'
  AND (report_role IS NULL OR trim(report_role) = '');

UPDATE cls_category SET report_role = 'liability'
WHERE coalesce(deleted, 0) = 0 AND code = 'LIABILITY'
  AND (report_role IS NULL OR trim(report_role) = '');

UPDATE cls_category SET report_role = 'investment'
WHERE coalesce(deleted, 0) = 0 AND code IN ('INVEST', 'WEALTH', 'FP')
  AND (report_role IS NULL OR trim(report_role) = '');

UPDATE cls_category SET report_role = 'cashflow'
WHERE coalesce(deleted, 0) = 0 AND code IN ('FEE', 'FE')
  AND (report_role IS NULL OR trim(report_role) = '');

UPDATE cls_category SET report_role = 'budget'
WHERE coalesce(deleted, 0) = 0 AND code IN ('FIXED', 'LIVING', 'SHOPPING', 'TRANSPORT', 'TRAVEL', 'EDU', 'ENT', 'GIFT', 'SOCIAL', 'OTHER')
  AND (report_role IS NULL OR trim(report_role) = '');

-- 2) semantic_tag — catalog specials (non-default semantics)
UPDATE cls_category SET semantic_tag = 'subscription_spending'
WHERE coalesce(deleted, 0) = 0 AND code = 'FIXED-05'
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'essential_spending'
WHERE coalesce(deleted, 0) = 0 AND code IN ('FIXED-04', 'TRANS-06', 'DEBT-05', 'FEE-01', 'FEE-02', 'FEE-03', 'FEE-04', 'FEE-05', 'EDU-01')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'fixed_spending'
WHERE coalesce(deleted, 0) = 0 AND code IN ('FIXED-01', 'FIXED-02', 'FIXED-03', 'FIXED-06', 'FIXED-07', 'FIXED-99')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'investment_income'
WHERE coalesce(deleted, 0) = 0 AND code IN ('INC-04', 'INCOME-03', 'INV-04', 'INV-06', 'WEALTH-02')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'real_income'
WHERE coalesce(deleted, 0) = 0 AND code IN (
    'INC-01', 'INC-02', 'INC-03', 'INC-05', 'INC-06', 'INC-07', 'INC-09', 'INC-99',
    'INCOME-01', 'INCOME-02', 'INCOME-99'
) AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'refund_reimbursement'
WHERE coalesce(deleted, 0) = 0 AND (
    code IN ('INC-10', 'REIM-01', 'REIM-02', 'REIM-03', 'REIM-04', 'REIM-05')
    OR parent_id IN ('REIM', 'REIMB')
) AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'transfer'
WHERE coalesce(deleted, 0) = 0 AND code IN ('GIFT-02', 'ASSET-02', 'INVEST-05')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'liability'
WHERE coalesce(deleted, 0) = 0 AND code IN ('INC-08', 'DEBT-01', 'DEBT-02', 'DEBT-03', 'DEBT-04')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'investment'
WHERE coalesce(deleted, 0) = 0 AND code IN (
    'INV-01', 'INV-02', 'INV-03', 'INV-05',
    'WEALTH-01', 'WEALTH-03', 'WEALTH-04', 'WEALTH-05',
    'INVEST-01', 'INVEST-02', 'INVEST-03'
) AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'asset_adjustment'
WHERE coalesce(deleted, 0) = 0 AND code IN ('ASSET-01', 'ASSET-03', 'ASSET-04', 'ASSET-05')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'social_spending'
WHERE coalesce(deleted, 0) = 0 AND (
    parent_id IN ('GIFT', 'SOCIAL') OR code LIKE 'GIFT-%'
) AND code NOT IN ('GIFT-02')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'other_expense'
WHERE coalesce(deleted, 0) = 0 AND (
    parent_id = 'OTHER' OR code LIKE 'OTHER-%'
) AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

-- 3) semantic_tag — L1 roots
UPDATE cls_category SET semantic_tag = 'real_income'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code IN ('INC', 'INCOME')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'fixed_spending'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code = 'FIXED'
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'daily_spending'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code IN ('LIVING', 'SHOPPING', 'TRANSPORT', 'TRAVEL', 'ENT', 'EDU')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'social_spending'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code IN ('GIFT', 'SOCIAL')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'refund_reimbursement'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code IN ('REIM', 'REIMB')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'asset_adjustment'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code = 'ASSET'
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'liability'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code = 'LIABILITY'
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'investment'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code IN ('INVEST', 'WEALTH', 'FP')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'essential_spending'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code IN ('FEE', 'FE')
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category SET semantic_tag = 'other_expense'
WHERE coalesce(deleted, 0) = 0 AND level <= 1 AND code = 'OTHER'
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

-- 4) semantic_tag — L2 by parent (custom + catalog children still blank)
UPDATE cls_category c
SET c.semantic_tag = 'fixed_spending'
WHERE coalesce(c.deleted, 0) = 0
  AND c.parent_id = 'FIXED'
  AND c.code <> 'FIXED-05'
  AND (c.semantic_tag IS NULL OR trim(c.semantic_tag) = '');

UPDATE cls_category c
SET c.semantic_tag = 'daily_spending'
WHERE coalesce(c.deleted, 0) = 0
  AND c.parent_id IN ('LIVING', 'SHOPPING', 'TRANSPORT', 'TRAVEL', 'ENT')
  AND (c.semantic_tag IS NULL OR trim(c.semantic_tag) = '');

UPDATE cls_category c
SET c.semantic_tag = 'daily_spending'
WHERE coalesce(c.deleted, 0) = 0
  AND c.parent_id = 'EDU'
  AND c.code <> 'EDU-01'
  AND (c.semantic_tag IS NULL OR trim(c.semantic_tag) = '');

UPDATE cls_category c
SET c.semantic_tag = 'real_income'
WHERE coalesce(c.deleted, 0) = 0
  AND c.parent_id IN ('INC', 'INCOME')
  AND c.code NOT IN ('INC-04', 'INC-08', 'INC-10', 'INCOME-03')
  AND (c.semantic_tag IS NULL OR trim(c.semantic_tag) = '');

UPDATE cls_category c
SET c.semantic_tag = 'liability'
WHERE coalesce(c.deleted, 0) = 0
  AND c.parent_id = 'LIABILITY'
  AND c.code NOT IN ('DEBT-05')
  AND (c.semantic_tag IS NULL OR trim(c.semantic_tag) = '');

UPDATE cls_category c
SET c.semantic_tag = 'investment'
WHERE coalesce(c.deleted, 0) = 0
  AND c.parent_id IN ('INVEST', 'WEALTH', 'FP')
  AND c.code NOT IN ('INV-04', 'INV-06', 'WEALTH-02', 'INVEST-05')
  AND (c.semantic_tag IS NULL OR trim(c.semantic_tag) = '');

UPDATE cls_category c
SET c.semantic_tag = 'essential_spending'
WHERE coalesce(c.deleted, 0) = 0
  AND c.parent_id IN ('FEE', 'FE')
  AND (c.semantic_tag IS NULL OR trim(c.semantic_tag) = '');

-- 5) remaining blanks → other
UPDATE cls_category
SET semantic_tag = 'other'
WHERE coalesce(deleted, 0) = 0
  AND (semantic_tag IS NULL OR trim(semantic_tag) = '');

UPDATE cls_category
SET report_role = 'other'
WHERE coalesce(deleted, 0) = 0
  AND (report_role IS NULL OR trim(report_role) = '');
