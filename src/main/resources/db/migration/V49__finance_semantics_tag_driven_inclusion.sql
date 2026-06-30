-- Derive finance-semantics inclusion and economic nature from Reporting Classification (semantic_tag),
-- with report_role fallback when semantic_tag is missing or 'other'.

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
    CASE COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other')
        WHEN 'real_income' THEN 'income'
        WHEN 'investment_income' THEN 'income'
        WHEN 'other_income' THEN 'income'
        WHEN 'refund_reimbursement' THEN 'refund'
        WHEN 'tax_refund' THEN 'refund'
        WHEN 'tax_expense' THEN 'expense'
        WHEN 'transfer' THEN 'transfer'
        WHEN 'finance_loan' THEN 'liability'
        WHEN 'finance_credit_loan' THEN 'liability'
        WHEN 'finance_installment' THEN 'liability'
        WHEN 'liability' THEN 'liability'
        WHEN 'investment' THEN 'investment'
        WHEN 'asset_adjustment' THEN 'asset_adjustment'
        WHEN 'other' THEN CASE
            WHEN v.is_transfer = 1 THEN 'transfer'
            WHEN v.is_refund = 1 OR COALESCE(cat.report_role, '') = 'refund' THEN 'refund'
            WHEN COALESCE(cat.report_role, '') = 'liability' THEN 'liability'
            WHEN COALESCE(cat.report_role, '') = 'investment' THEN 'investment'
            WHEN COALESCE(cat.report_role, '') = 'asset' THEN 'asset_adjustment'
            WHEN v.direction = 'income' THEN 'income'
            WHEN v.direction = 'expense' THEN 'expense'
            ELSE 'other'
        END
        ELSE CASE
            WHEN v.is_transfer = 1 THEN 'transfer'
            WHEN v.direction = 'income' THEN 'income'
            WHEN v.direction = 'expense' THEN 'expense'
            ELSE 'other'
        END
    END AS economic_nature,
    CASE
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') IN (
            'fixed_housing', 'fixed_utilities', 'fixed_telecom', 'fixed_insurance',
            'fixed_tuition', 'fixed_repayment', 'fixed_misc', 'fixed_spending',
            'subscription_spending'
        ) THEN 'fixed'
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') IN (
            'essential_spending', 'finance_fee', 'tax_expense'
        ) THEN 'essential'
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') IN (
            'dining_spending', 'groceries_spending', 'shopping_spending', 'transport_spending',
            'entertainment_spending', 'education_spending', 'medical_spending',
            'social_spending', 'daily_spending', 'other_expense'
        ) THEN 'variable'
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') = 'other' THEN CASE
            WHEN v.fixed_flag = 1 THEN 'fixed'
            WHEN COALESCE(cat.report_role, '') = 'cashflow' THEN 'essential'
            WHEN v.category_code = '__UNCLASSIFIED__' OR v.category_code IS NULL OR v.category_code = '' THEN 'unclassified'
            WHEN v.direction = 'expense' AND COALESCE(cat.report_role, '') IN ('budget', 'other') THEN 'variable'
            ELSE 'variable'
        END
        ELSE 'variable'
    END AS budget_behavior,
    CASE
        WHEN v.category_code = '__UNCLASSIFIED__' OR v.category_code IS NULL OR v.category_code = '' THEN 'unclassified'
        WHEN v.is_refund = 1 THEN 'inferred'
        ELSE 'classified'
    END AS quality_state,
    CASE
        WHEN v.is_transfer = 1 THEN 0
        WHEN v.is_refund = 1 THEN 0
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') IN (
            'real_income', 'investment_income', 'other_income'
        ) AND v.direction = 'income' THEN 1
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') = 'other'
            AND COALESCE(cat.report_role, '') = 'income'
            AND v.direction = 'income'
            AND v.is_refund = 0 THEN 1
        ELSE 0
    END AS include_in_income_trend,
    CASE
        WHEN v.is_transfer = 1 THEN 0
        WHEN v.is_refund = 1 THEN 0
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') IN (
            'dining_spending', 'groceries_spending', 'shopping_spending', 'transport_spending',
            'entertainment_spending', 'education_spending', 'medical_spending', 'social_spending',
            'subscription_spending', 'essential_spending', 'finance_fee', 'daily_spending',
            'other_expense', 'fixed_housing', 'fixed_utilities', 'fixed_telecom', 'fixed_insurance',
            'fixed_tuition', 'fixed_repayment', 'fixed_misc', 'fixed_spending', 'tax_expense'
        ) AND v.direction = 'expense' THEN 1
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') = 'other'
            AND COALESCE(cat.report_role, '') NOT IN ('refund', 'liability', 'investment', 'transfer', 'asset')
            AND v.direction = 'expense' THEN 1
        ELSE 0
    END AS include_in_expense_trend,
    CASE
        WHEN v.is_transfer = 1 THEN 0
        WHEN v.is_refund = 1 THEN 0
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') IN (
            'transfer', 'finance_loan', 'finance_credit_loan', 'finance_installment',
            'investment', 'asset_adjustment', 'liability'
        ) THEN 0
        WHEN COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') = 'other'
            AND COALESCE(cat.report_role, '') IN ('refund', 'liability', 'investment', 'transfer', 'asset') THEN 0
        WHEN v.direction = 'expense'
            AND (
                v.fixed_flag = 1
                OR COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') IN (
                    'fixed_housing', 'fixed_utilities', 'fixed_telecom', 'fixed_insurance',
                    'fixed_tuition', 'fixed_repayment', 'fixed_misc', 'fixed_spending',
                    'subscription_spending', 'essential_spending', 'finance_fee', 'tax_expense',
                    'dining_spending', 'groceries_spending', 'shopping_spending', 'transport_spending',
                    'entertainment_spending', 'education_spending', 'medical_spending',
                    'social_spending', 'daily_spending', 'other_expense'
                )
                OR (
                    COALESCE(NULLIF(TRIM(cat.semantic_tag), ''), 'other') = 'other'
                    AND COALESCE(cat.report_role, '') IN ('budget', 'cashflow')
                )
            ) THEN 1
        ELSE 0
    END AS include_in_budget,
    CASE WHEN v.is_transfer = 0 THEN 1 ELSE 0 END AS include_in_cashflow,
    CASE WHEN v.is_transfer = 0 AND v.category_code != '__UNCLASSIFIED__' THEN 1 ELSE 0 END AS include_in_profile
FROM v_transaction_analytics v
LEFT JOIN cls_category cat ON cat.code = v.category_code AND cat.deleted != 1;
