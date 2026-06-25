import { describe, expect, it } from 'vitest'
import { mergeDashboardPeriodTotals } from './dashboardMetrics'

describe('mergeDashboardPeriodTotals', () => {
  it('prefers semantic totals when present', () => {
    const out = mergeDashboardPeriodTotals(
      {
        realIncome: 10000,
        consumptionExpense: 7000,
        netCashflow: 3000,
        metricsSource: 'v_transaction_finance_semantics',
        months: [{ yearMonth: '2026-01', month: 'Jan', realIncome: 5000, consumptionExpense: 3500, net: 1500 }],
      },
      { income: 12000, expense: 8000, months: [] },
    )
    expect(out.realIncome).toBe(10000)
    expect(out.usedSemantic).toBe(true)
  })

  it('falls back to report totals when semantic is empty', () => {
    const out = mergeDashboardPeriodTotals(
      { realIncome: 0, consumptionExpense: 0, netCashflow: 0, metricsSource: 'x', months: [] },
      { income: 5000, expense: 3000, months: [{ month: 'Jan', income: 5000, expense: 3000 }] },
    )
    expect(out.realIncome).toBe(5000)
    expect(out.usedSemantic).toBe(false)
  })
})
