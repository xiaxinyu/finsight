import { describe, expect, it } from 'vitest'
import {
  coerceCategoryFormFields,
  fixedCostKindLabel,
  inferDefaultReportRole,
  isFixedCategory,
  isFixedCostCategoryCode,
  profileCategorySemantics,
  reportRoleFromSemanticSelection,
  semanticTagFromReportRole,
  semanticTagLabel,
  filterSemanticTagGroups,
  SEMANTIC_TAG_GROUPS,
} from './categorySemantics'

describe('categorySemantics', () => {
  it('labels semantic tags', () => {
    expect(semanticTagLabel('fixed_spending')).toBe('Fixed')
    expect(semanticTagLabel('subscription_spending')).toBe('Subscription')
    expect(semanticTagLabel('social_spending')).toBe('Social')
    expect(semanticTagLabel('daily_spending')).toBe('Discretionary')
    expect(fixedCostKindLabel('rent')).toBe('Housing')
  })

  it('infers default report role for salary income', () => {
    expect(inferDefaultReportRole('INC', 'INC-01', 'income')).toBe('income')
    expect(semanticTagFromReportRole(undefined, 'INC', 'INC-01', 'income')).toBe('real_income')
  })

  it('filters groups by transaction type', () => {
    const incomeOnly = filterSemanticTagGroups(SEMANTIC_TAG_GROUPS, 'income')
    expect(incomeOnly.some((g) => g.title === 'Income')).toBe(true)
    expect(incomeOnly.some((g) => g.appliesTo === 'expense' && g.title === 'Expense')).toBe(false)
  })

  it('maps semantic tags to stored report roles', () => {
    expect(reportRoleFromSemanticSelection('daily_spending')).toBe('budget')
    expect(reportRoleFromSemanticSelection('social_spending')).toBe('budget')
    expect(reportRoleFromSemanticSelection('subscription_spending', 'subscription')).toBe('budget')
    expect(reportRoleFromSemanticSelection('fixed_spending', 'insurance')).toBe('cashflow')
    expect(reportRoleFromSemanticSelection('fixed_spending', 'repayment')).toBe('liability')
  })

  it('derives semantic tag from report role and category tree', () => {
    expect(semanticTagFromReportRole('budget', 'FIXED', 'FIXED-05', 'expense')).toBe('subscription_spending')
    expect(semanticTagFromReportRole('budget', 'GIFT', 'GIFT-01', 'expense')).toBe('social_spending')
    expect(semanticTagFromReportRole('budget', 'LIVING', 'DAILY-01', 'expense')).toBe('daily_spending')
    expect(semanticTagFromReportRole('budget', 'FIXED', 'FIXED-01', 'expense')).toBe('fixed_spending')
    expect(semanticTagFromReportRole('income', 'INC', 'INC-01', 'income')).toBe('real_income')
  })

  it('previews budget expense as variable for daily spending', () => {
    const p = profileCategorySemantics('budget', 'expense', 'LIVING', 'DAILY-01')
    expect(p.includeInExpenseTrend).toBe(true)
    expect(p.includeInBudget).toBe(true)
    expect(p.budgetBehavior).toBe('variable')
    expect(p.semanticTag).toBe('daily_spending')
  })

  it('previews salary as real income', () => {
    const p = profileCategorySemantics(undefined, 'income', 'INC', 'INC-01')
    expect(p.semanticTag).toBe('real_income')
    expect(p.includeInIncomeTrend).toBe(true)
  })

  it('detects fixed cost category codes for transactions', () => {
    expect(isFixedCostCategoryCode('FIXED-01')).toBe(true)
    expect(isFixedCategory('FIXED', 'FIXED-02')).toBe(true)
  })

  it('coerces fixed rent category away from income + investment role', () => {
    const coerced = coerceCategoryFormFields({
      parentId: 'FIXED',
      code: 'FIXED-01',
      txnTypes: 'income',
      reportRole: 'investment',
    })
    expect(coerced.txnTypes).toBe('expense')
    expect(coerced.reportRole).toBe('budget')
    expect(coerced.warnings.length).toBeGreaterThan(0)
    const preview = profileCategorySemantics(coerced.reportRole, coerced.txnTypes, 'FIXED', 'FIXED-01')
    expect(preview.semanticTag).toBe('fixed_spending')
    expect(preview.includeInFixedCostReport).toBe(true)
  })

  it('previews expense category without income classification when report_role is stale', () => {
    const coerced = coerceCategoryFormFields({
      parentId: 'INC',
      code: 'DAILY-01',
      txnTypes: 'expense',
      reportRole: 'income',
    })
    expect(coerced.reportRole).toBe('budget')
    const preview = profileCategorySemantics('income', 'expense', 'INC', 'DAILY-01')
    expect(preview.semanticTag).toBe('daily_spending')
    expect(preview.includeInIncomeTrend).toBe(false)
    expect(preview.includeInExpenseTrend).toBe(true)
  })
})
