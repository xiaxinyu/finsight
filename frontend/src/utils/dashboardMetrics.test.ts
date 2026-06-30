import { describe, expect, it } from 'vitest'
import { mapDashboardPeriodTotals, mergeDashboardPeriodTotals } from './dashboardMetrics'
import { REPORT_METRICS_SOURCE } from './reportTaxonomy'

describe('dashboardMetrics', () => {
  it('maps semantic period summary', () => {
    const out = mapDashboardPeriodTotals({
      realIncome: 10000,
      consumptionExpense: 7000,
      netCashflow: 3000,
      metricsSource: 'v_transaction_finance_semantics',
      months: [{ yearMonth: '2026-01', month: 'Jan', realIncome: 5000, consumptionExpense: 3500, net: 1500 }],
    })
    expect(out.realIncome).toBe(10000)
    expect(out.metricsSource).toBe('v_transaction_finance_semantics')
  })

  it('defaults metrics source when absent', () => {
    const out = mapDashboardPeriodTotals(undefined)
    expect(out.metricsSource).toBe(REPORT_METRICS_SOURCE)
  })

  it('mergeDashboardPeriodTotals delegates to semantic mapper', () => {
    const out = mergeDashboardPeriodTotals(
      { realIncome: 5000, consumptionExpense: 3000, netCashflow: 2000, metricsSource: 'x', months: [] },
    )
    expect(out.realIncome).toBe(5000)
    expect(out.usedSemantic).toBe(true)
  })
})
