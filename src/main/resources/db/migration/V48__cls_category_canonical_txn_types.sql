-- Canonical txn_types aligned with admin Transaction type kinds (Expense / Income / Finance / Transfer / Tax / Refund).
-- Preserves SQL LIKE filters (%invest%, %liability%, %transfer%) via composite tokens.

-- 1) L1 finance roots
UPDATE cls_category
SET txn_types = 'finance,invest,liability'
WHERE COALESCE(deleted, 0) = 0
  AND level <= 1
  AND code IN ('WEALTH', 'INVEST', 'FP', 'LIABILITY')
  AND txn_types NOT LIKE '%finance%';

-- 2) L2 finance / capital flows (by semantic_tag)
UPDATE cls_category
SET txn_types = 'finance,invest,liability'
WHERE COALESCE(deleted, 0) = 0
  AND semantic_tag IN (
    'investment',
    'finance_loan',
    'finance_credit_loan',
    'finance_installment',
    'liability'
  )
  AND semantic_tag <> 'fixed_repayment'
  AND txn_types NOT LIKE '%finance%';

-- Borrowing inflow under income tree
UPDATE cls_category
SET txn_types = 'finance,invest,liability'
WHERE COALESCE(deleted, 0) = 0
  AND code IN ('INC-08', 'DEBT-01', 'DEBT-02', 'DEBT-03', 'DEBT-04')
  AND txn_types NOT LIKE '%finance%';

-- Investment activity leaves (legacy codes without semantic_tag backfill)
UPDATE cls_category
SET txn_types = 'finance,invest,liability'
WHERE COALESCE(deleted, 0) = 0
  AND (
    parent_id IN ('WEALTH', 'INVEST', 'FP', 'LIABILITY')
    OR code LIKE 'WEALTH-%'
    OR code LIKE 'INVEST-%'
    OR code LIKE 'DEBT-%'
    OR code LIKE 'INV-%'
  )
  AND code NOT IN ('WEALTH-02')
  AND txn_types NOT LIKE '%finance%';

-- Portfolio income (P&L dividends / interest) — income + invest tokens
UPDATE cls_category
SET txn_types = 'income,invest'
WHERE COALESCE(deleted, 0) = 0
  AND semantic_tag = 'investment_income'
  AND txn_types NOT LIKE '%income%';

-- 3) Transfer / asset adjustment
UPDATE cls_category
SET txn_types = 'transfer,asset'
WHERE COALESCE(deleted, 0) = 0
  AND semantic_tag = 'asset_adjustment'
  AND txn_types NOT LIKE '%transfer%';

UPDATE cls_category
SET txn_types = 'transfer,asset'
WHERE COALESCE(deleted, 0) = 0
  AND semantic_tag = 'transfer'
  AND parent_id = 'ASSET'
  AND txn_types NOT LIKE '%transfer%';

UPDATE cls_category
SET txn_types = 'transfer,asset'
WHERE COALESCE(deleted, 0) = 0
  AND level <= 1
  AND code = 'ASSET'
  AND txn_types NOT LIKE '%transfer%';

-- Pure internal transfer (no asset token required for parse, normalize to canonical)
UPDATE cls_category
SET txn_types = 'transfer,asset'
WHERE COALESCE(deleted, 0) = 0
  AND code = 'ASSET-02'
  AND txn_types = 'transfer';

-- 4) Tax
UPDATE cls_category
SET txn_types = 'tax,expense,income'
WHERE COALESCE(deleted, 0) = 0
  AND semantic_tag IN ('tax_expense', 'tax_refund')
  AND txn_types NOT LIKE '%tax%';

-- 5) Refund / reimbursement
UPDATE cls_category
SET txn_types = 'income,refund'
WHERE COALESCE(deleted, 0) = 0
  AND (
    semantic_tag = 'refund_reimbursement'
    OR parent_id IN ('REIM', 'REIMB')
    OR code IN ('INC-10', 'REIM-01', 'REIM-02', 'REIM-03', 'REIM-04', 'REIM-05')
  )
  AND txn_types NOT LIKE '%refund%';

UPDATE cls_category
SET txn_types = 'income,refund'
WHERE COALESCE(deleted, 0) = 0
  AND level <= 1
  AND code IN ('REIM', 'REIMB')
  AND txn_types NOT LIKE '%refund%';
