import { describe, expect, it } from 'vitest'
import { inclusionSummary, profileCategorySemantics, reportRoleLabel } from './categorySemantics'

describe('categorySemantics', () => {
  it('labels report roles', () => {
    expect(reportRoleLabel('refund')).toBe('Refund / reimbursement')
  })

  it('summarizes inclusion flags', () => {
    expect(inclusionSummary({
      includeInIncomeTrend: true,
      includeInExpenseTrend: false,
      includeInBudget: false,
    })).toContain('income trend')
  })

  it('previews budget expense inclusion', () => {
    const p = profileCategorySemantics('budget', 'expense')
    expect(p.includeInExpenseTrend).toBe(true)
    expect(p.includeInBudget).toBe(true)
    expect(p.includeInIncomeTrend).toBe(false)
  })

  it('previews investment exclusion', () => {
    const p = profileCategorySemantics('investment', 'expense,invest')
    expect(p.includeInExpenseTrend).toBe(false)
    expect(p.economicNature).toBe('investment')
  })
})
