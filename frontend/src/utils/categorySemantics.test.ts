import { describe, expect, it } from 'vitest'
import {
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
    expect(semanticTagLabel('fixed_spending')).toBe('Fixed Cost')
    expect(semanticTagLabel('subscription_spending')).toBe('Subscription')
    expect(fixedCostKindLabel('rent')).toBe('Rent And Mortgage')
  })

  it('infers default report role for salary income', () => {
    expect(inferDefaultReportRole('INC', 'INC-01', 'income')).toBe('income')
    expect(semanticTagFromReportRole(undefined, 'INC', 'INC-01', 'income')).toBe('real_income')
  })

  it('filters groups by transaction type', () => {
    const incomeOnly = filterSemanticTagGroups(SEMANTIC_TAG_GROUPS, 'income')
    expect(incomeOnly.some((g) => g.title.includes('Income'))).toBe(true)
    expect(incomeOnly.some((g) => g.appliesTo === 'expense' && g.title.includes('Expense'))).toBe(false)
  })

  it('maps semantic tags to stored report roles', () => {
    expect(reportRoleFromSemanticSelection('daily_spending')).toBe('budget')
    expect(reportRoleFromSemanticSelection('subscription_spending', 'subscription')).toBe('budget')
    expect(reportRoleFromSemanticSelection('fixed_spending', 'insurance')).toBe('cashflow')
    expect(reportRoleFromSemanticSelection('fixed_spending', 'repayment')).toBe('liability')
  })

  it('derives semantic tag from report role and category tree', () => {
    expect(semanticTagFromReportRole('budget', 'FIXED', 'FIXED-05', 'expense')).toBe('subscription_spending')
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
})
