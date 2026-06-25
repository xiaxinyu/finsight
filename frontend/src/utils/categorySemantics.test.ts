import { describe, expect, it } from 'vitest'
import { inclusionSummary, reportRoleLabel } from './categorySemantics'

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
})
