import { describe, expect, it } from 'vitest'
import {
  insightsSemanticStructure,
  isDrillableSemanticTag,
  semanticBreakdownToReportPoints,
  topSemanticRows,
  type SemanticBreakdown,
} from './semanticBreakdownReport'

describe('semanticBreakdownReport', () => {
  it('maps rows to report points by classification path', () => {
    const pts = semanticBreakdownToReportPoints([
      { tagId: 'dining_spending', label: 'Dining', classification: 'Expense / Dining', amount: 100 },
    ])
    expect(pts[0]).toEqual({ key: 'dining_spending', value: 100, code: 'dining_spending', name: 'Expense / Dining' })
  })

  it('rolls excess rows into Other', () => {
    const rows = Array.from({ length: 12 }, (_, i) => ({
      tagId: `tag_${i}`,
      label: `Tag ${i}`,
      classL1: 'Expense',
      classL2: `Tag ${i}`,
      classification: `Expense / Tag ${i}`,
      txnType: 'Expense' as const,
      group: 'expense' as const,
      amount: 100 - i,
      sharePct: 10,
    }))
    const top = topSemanticRows(rows, 10)
    expect(top).toHaveLength(11)
    expect(top[10]?.label).toBe('Other')
  })

  it('blocks drill on rolled-up Other slice', () => {
    expect(isDrillableSemanticTag('other_combined')).toBe(false)
    expect(isDrillableSemanticTag('dining_spending')).toBe(true)
  })

  it('builds fixed vs variable insights', () => {
    const breakdown: SemanticBreakdown = {
      rows: [
        {
          tagId: 'fixed_housing',
          label: 'Housing',
          classL1: 'Fixed',
          classL2: 'Housing',
          classification: 'Fixed / Housing',
          txnType: 'Expense',
          group: 'fixed',
          amount: 5000,
          sharePct: 50,
        },
        {
          tagId: 'dining_spending',
          label: 'Dining',
          classL1: 'Expense',
          classL2: 'Dining',
          classification: 'Expense / Dining',
          txnType: 'Expense',
          group: 'expense',
          amount: 5000,
          sharePct: 50,
        },
      ],
      expenseTotal: 10000,
      fixedTotal: 5000,
      variableTotal: 5000,
      fixedSharePct: 50,
      variableSharePct: 50,
    }
    const bullets = insightsSemanticStructure(breakdown, 'Jan 2026')
    expect(bullets.some((b) => b.text.includes('fixed 50%'))).toBe(true)
    expect(bullets.some((b) => b.warn)).toBe(true)
  })
})
