-- Map liability / debt categories to Finance semantic tags (Loan, Credit loan, Installment).

UPDATE cls_category
SET semantic_tag = 'finance_credit_loan'
WHERE code = 'DEBT-01'
  AND COALESCE(deleted, 0) = 0;

UPDATE cls_category
SET semantic_tag = 'finance_loan'
WHERE code IN ('DEBT-02', 'DEBT-03', 'INC-08')
  AND COALESCE(deleted, 0) = 0;

UPDATE cls_category
SET semantic_tag = 'finance_installment'
WHERE code = 'DEBT-04'
  AND COALESCE(deleted, 0) = 0;

UPDATE cls_category
SET semantic_tag = 'finance_loan'
WHERE code = 'LIABILITY'
  AND level <= 1
  AND COALESCE(deleted, 0) = 0
  AND (semantic_tag IS NULL OR semantic_tag IN ('liability', 'other'));

UPDATE cls_category
SET semantic_tag = 'finance_loan'
WHERE parent_id = 'LIABILITY'
  AND COALESCE(deleted, 0) = 0
  AND semantic_tag = 'liability'
  AND code NOT IN ('DEBT-01', 'DEBT-04');
