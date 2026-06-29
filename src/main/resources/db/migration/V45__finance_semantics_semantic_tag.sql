-- Expose category semantic_tag on finance semantics view for report aggregation.

CREATE OR REPLACE VIEW v_transaction_finance_semantics AS
SELECT
    v.id,
    v.txn_date,
    v.txn_kind,
    v.direction,
    v.amount,
    v.category_code,
    v.category_name,
    v.consume_l1,
    v.consume_l2,
    v.category_l1_code,
    v.category_l1_name,
    v.bank_code,
    v.card_type_code,
    v.transaction_desc,
    v.opponent_name,
    v.memo,
    v.merchant_token,
    v.is_transfer,
    v.is_refund,
    v.fixed_flag,
    v.statement_id,
    COALESCE(NULLIF(TRIM(cat.report_role), ''), 'other') AS report_role,
    COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') AS semantic_tag,
    CASE
        WHEN v.is_transfer = 1 THEN 'neutral'
        WHEN v.direction = 'income' THEN 'inflow'
        WHEN v.direction = 'expense' THEN 'outflow'
        ELSE 'neutral'
    END AS cash_direction,
    CASE
        WHEN v.is_transfer = 1 THEN 'transfer'
        WHEN v.is_refund = 1 OR COALESCE(cat.report_role, '') = 'refund' THEN 'refund'
        WHEN COALESCE(cat.report_role, '') = 'liability' THEN 'liability'
        WHEN COALESCE(cat.report_role, '') = 'investment' THEN 'investment'
        WHEN COALESCE(cat.report_role, '') = 'asset' THEN 'asset_adjustment'
        WHEN v.direction = 'income' THEN 'income'
        WHEN v.direction = 'expense' THEN 'expense'
        ELSE 'other'
    END AS economic_nature,
    CASE
        WHEN v.fixed_flag = 1 THEN 'fixed'
        WHEN COALESCE(cat.report_role, '') = 'cashflow' THEN 'essential'
        WHEN v.category_code = '__UNCLASSIFIED__' OR v.category_code IS NULL OR v.category_code = '' THEN 'unclassified'
        WHEN v.direction = 'expense' AND COALESCE(cat.report_role, '') IN ('budget', 'other') THEN 'variable'
        ELSE 'variable'
    END AS budget_behavior,
    CASE
        WHEN v.category_code = '__UNCLASSIFIED__' OR v.category_code IS NULL OR v.category_code = '' THEN 'unclassified'
        WHEN v.is_refund = 1 THEN 'inferred'
        ELSE 'classified'
    END AS quality_state,
    CASE
        WHEN v.is_transfer = 1 THEN 0
        WHEN v.direction = 'income'
            AND v.is_refund = 0
            AND COALESCE(cat.report_role, '') = 'income' THEN 1
        ELSE 0
    END AS include_in_income_trend,
    CASE
        WHEN v.is_transfer = 1 THEN 0
        WHEN v.is_refund = 1 THEN 0
        WHEN COALESCE(cat.report_role, '') IN ('refund', 'liability', 'investment', 'transfer', 'asset') THEN 0
        WHEN v.direction = 'expense' THEN 1
        ELSE 0
    END AS include_in_expense_trend,
    CASE
        WHEN v.is_transfer = 1 THEN 0
        WHEN v.is_refund = 1 THEN 0
        WHEN COALESCE(cat.report_role, '') IN ('refund', 'liability', 'investment', 'transfer', 'asset') THEN 0
        WHEN v.direction = 'expense'
            AND (v.fixed_flag = 1 OR COALESCE(cat.report_role, '') IN ('budget', 'cashflow')) THEN 1
        ELSE 0
    END AS include_in_budget,
    CASE WHEN v.is_transfer = 0 THEN 1 ELSE 0 END AS include_in_cashflow,
    CASE WHEN v.is_transfer = 0 AND v.category_code != '__UNCLASSIFIED__' THEN 1 ELSE 0 END AS include_in_profile
FROM v_transaction_analytics v
LEFT JOIN cls_category cat ON cat.code = v.category_code AND cat.deleted != 1;
