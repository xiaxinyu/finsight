import { describe, expect, it } from 'vitest'
import {
  categoryFieldsFromSemanticTag,
  coerceCategoryFormFields,
  fixedCostKindLabel,
  inferDefaultReportRole,
  isFixedCategory,
  isFixedCostCategoryCode,
  profileCategorySemantics,
  reportRoleFromSemanticSelection,
  resolveSemanticTag,
  semanticTagFromReportRole,
  semanticTagLabel,
  filterSemanticTagGroups,
  SEMANTIC_TAG_GROUPS,
} from './categorySemantics'

describe('categorySemantics', () => {
  it('labels semantic tags', () => {
    expect(semanticTagLabel('fixed_spending')).toBe('Fixed')
    expect(semanticTagLabel('shopping_spending')).toBe('Shopping')
    expect(semanticTagLabel('dining_spending')).toBe('Dining')
    expect(semanticTagLabel('transport_spending')).toBe('Transport')
    expect(semanticTagLabel('daily_spending')).toBe('General')
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
    expect(semanticTagFromReportRole('budget', 'LIVING', 'DAILY-01', 'expense')).toBe('dining_spending')
    expect(semanticTagFromReportRole('budget', 'LIVING', 'LIVING-03', 'expense', '超市购物 (食材、粮油、日用品)')).toBe('shopping_spending')
    expect(semanticTagFromReportRole('budget', 'FIXED', 'FIXED-01', 'expense')).toBe('fixed_spending')
    expect(semanticTagFromReportRole('income', 'INC', 'INC-01', 'income')).toBe('real_income')
  })

  it('previews budget expense as variable for daily spending', () => {
    const p = profileCategorySemantics('budget', 'expense', 'LIVING', 'DAILY-01')
    expect(p.includeInExpenseTrend).toBe(true)
    expect(p.includeInBudget).toBe(true)
    expect(p.budgetBehavior).toBe('variable')
    expect(p.semanticTag).toBe('dining_spending')
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
    expect(preview.semanticTag).toBe('dining_spending')
    expect(preview.includeInIncomeTrend).toBe(false)
    expect(preview.includeInExpenseTrend).toBe(true)
  })

  it('keeps stored semantic tag over report_role inference', () => {
    expect(resolveSemanticTag('social_spending', 'budget', 'TRANSPORT', 'TRANS-01', 'expense')).toBe('social_spending')
    const preview = profileCategorySemantics('budget', 'expense', 'TRANSPORT', 'TRANS-01', 'social_spending')
    expect(preview.semanticTag).toBe('social_spending')
  })

  it('persists earned income from semantic tag selection', () => {
    const derived = categoryFieldsFromSemanticTag('real_income', {
      parentId: 'INC',
      code: 'INC-01',
      txnTypes: 'income',
    })
    expect(derived.semanticTag).toBe('real_income')
    expect(derived.reportRole).toBe('income')
    expect(derived.txnTypes).toBe('income')
  })
})
