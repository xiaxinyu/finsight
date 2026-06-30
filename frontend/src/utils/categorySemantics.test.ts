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
  inferExpenseDomainTag,
  SEMANTIC_TAG_GROUPS,
  shouldHideCapitalRow,
  visibleSemanticTagsForGroup,
} from './categorySemantics'

describe('categorySemantics', () => {
  it('labels semantic tags', () => {
    expect(semanticTagLabel('fixed_spending')).toBe('Fixed')
    expect(semanticTagLabel('shopping_spending')).toBe('Shopping')
    expect(semanticTagLabel('dining_spending')).toBe('Dining')
    expect(semanticTagLabel('transport_spending')).toBe('Transport')
    expect(semanticTagLabel('medical_spending')).toBe('Medical')
    expect(semanticTagLabel('daily_spending')).toBe('General')
    expect(fixedCostKindLabel('rent')).toBe('Housing')
  })

  it('infers default report role for salary income', () => {
    expect(inferDefaultReportRole('INC', 'INC-01', 'income')).toBe('income')
    expect(semanticTagFromReportRole(undefined, 'INC', 'INC-01', 'income')).toBe('real_income')
  })

  it('shows only income tags for reimbursement categories', () => {
    const reim = filterSemanticTagGroups(SEMANTIC_TAG_GROUPS, 'income,expense', 'REIM', 'REIM-01')
    expect(reim).toHaveLength(1)
    expect(reim[0]?.title).toBe('Income')
    expect(reim[0]?.tags).toContain('refund_reimbursement')
  })

  it('filters groups by transaction type', () => {
    const incomeOnly = filterSemanticTagGroups(SEMANTIC_TAG_GROUPS, 'income')
    expect(incomeOnly.some((g) => g.title === 'Income')).toBe(true)
    expect(incomeOnly.some((g) => g.appliesTo === 'expense' && g.title === 'Expense')).toBe(false)
  })

  it('hides transfer and finance rows for everyday expense categories', () => {
    expect(shouldHideCapitalRow('LIVING', 'LIVING-08', 'expense')).toBe(true)
    const living = filterSemanticTagGroups(SEMANTIC_TAG_GROUPS, 'expense', 'LIVING', 'LIVING-08')
    expect(living.some((g) => g.title === 'Transfer')).toBe(false)
    expect(living.some((g) => g.title === 'Finance')).toBe(false)
    expect(living.some((g) => g.title === 'Expense')).toBe(true)
    const withAdvanced = filterSemanticTagGroups(
      SEMANTIC_TAG_GROUPS, 'expense', 'LIVING', 'LIVING-08', { includeCapital: true },
    )
    expect(withAdvanced.some((g) => g.title === 'Transfer')).toBe(true)
    expect(withAdvanced.some((g) => g.title === 'Finance')).toBe(true)
  })

  it('keeps finance rows for asset and liability trees', () => {
    expect(shouldHideCapitalRow('ASSET', 'ASSET-01', 'expense')).toBe(false)
    const asset = filterSemanticTagGroups(SEMANTIC_TAG_GROUPS, 'expense', 'ASSET', 'ASSET-01')
    expect(asset.some((g) => g.title === 'Finance')).toBe(true)
  })

  it('subscription appears only under expense not fixed', () => {
    const fixed = SEMANTIC_TAG_GROUPS.find((g) => g.title === 'Fixed')
    const expense = SEMANTIC_TAG_GROUPS.find((g) => g.title === 'Expense')
    expect(fixed?.tags).not.toContain('subscription_spending')
    expect(expense?.tags).toContain('subscription_spending')
  })

  it('hides legacy debt from finance unless selected', () => {
    const finance = SEMANTIC_TAG_GROUPS.find((g) => g.title === 'Finance')!
    const visible = visibleSemanticTagsForGroup(finance, 'dining_spending')
    expect(visible).not.toContain('liability')
    const withLegacy = visibleSemanticTagsForGroup({ ...finance, tags: [...finance.tags, 'liability'] }, 'liability')
    expect(withLegacy).toContain('liability')
  })

  it('includes tax and fee tags', () => {
    expect(SEMANTIC_TAG_GROUPS.some((g) => g.title === 'Tax')).toBe(true)
    expect(SEMANTIC_TAG_GROUPS.find((g) => g.title === 'Expense')?.tags).toContain('finance_fee')
    expect(SEMANTIC_TAG_GROUPS.find((g) => g.title === 'Expense')?.tags).toContain('groceries_spending')
  })

  it('infers groceries for supermarket categories', () => {
    expect(inferExpenseDomainTag('LIVING', 'DAILY-03', '超市购物 (食材、粮油、日用品)')).toBe('groceries_spending')
  })

  it('maps debt and fee tags to report roles', () => {
    expect(semanticTagFromReportRole('liability', 'LIABILITY', 'DEBT-01', 'transfer,liability')).toBe('finance_credit_loan')
    expect(reportRoleFromSemanticSelection('finance_fee')).toBe('cashflow')
    expect(reportRoleFromSemanticSelection('tax_refund')).toBe('refund')
  })

  it('shows tax group without expanding transfer and finance', () => {
    const living = filterSemanticTagGroups(SEMANTIC_TAG_GROUPS, 'expense', 'LIVING', 'LIVING-08')
    expect(living.some((g) => g.title === 'Tax')).toBe(true)
    expect(living.some((g) => g.title === 'Transfer')).toBe(false)
  })

  it('maps semantic tags to stored report roles', () => {
    expect(reportRoleFromSemanticSelection('daily_spending')).toBe('budget')
    expect(reportRoleFromSemanticSelection('medical_spending')).toBe('budget')
    expect(reportRoleFromSemanticSelection('subscription_spending', 'subscription')).toBe('budget')
    expect(reportRoleFromSemanticSelection('fixed_insurance')).toBe('cashflow')
    expect(reportRoleFromSemanticSelection('fixed_repayment')).toBe('liability')
    expect(reportRoleFromSemanticSelection('fixed_housing')).toBe('budget')
    expect(reportRoleFromSemanticSelection('fixed_spending', 'insurance')).toBe('cashflow')
    expect(reportRoleFromSemanticSelection('fixed_spending', 'repayment')).toBe('liability')
  })

  it('infers medical from category name', () => {
    expect(semanticTagFromReportRole('budget', 'LIVING', 'DAILY-05', 'expense', '医疗药品')).toBe('medical_spending')
    expect(semanticTagFromReportRole('budget', 'LIVING', 'LIVING-06', 'expense', '基础医疗')).toBe('medical_spending')
    expect(semanticTagFromReportRole('budget', 'LIVING', 'LIVING-08', 'expense', '宠物支出（食品、医疗）')).toBe('daily_spending')
  })

  it('derives semantic tag from report role and category tree', () => {
    expect(semanticTagFromReportRole('budget', 'FIXED', 'FIXED-05', 'expense')).toBe('subscription_spending')
    expect(semanticTagFromReportRole('budget', 'GIFT', 'GIFT-01', 'expense')).toBe('social_spending')
    expect(semanticTagFromReportRole('budget', 'LIVING', 'DAILY-01', 'expense')).toBe('dining_spending')
    expect(semanticTagFromReportRole('budget', 'LIVING', 'LIVING-03', 'expense', '超市购物 (食材、粮油、日用品)')).toBe('groceries_spending')
    expect(semanticTagFromReportRole('budget', 'FIXED', 'FIXED-01', 'expense')).toBe('fixed_housing')
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
    expect(preview.semanticTag).toBe('fixed_housing')
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
