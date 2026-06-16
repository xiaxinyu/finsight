import { describe, expect, it } from 'vitest'
import {
  buildConcentrationChart,
  buildDriftChart,
  buildSubscriptionInsights,
  buildSubscriptionKpis,
  type MerchantConcentrationReport,
  type MerchantDriftReport,
  type SubscriptionReport,
} from './merchantReports'

const subscriptionReport: SubscriptionReport = {
  subscriptions: [
    {
      merchantToken: 'netflix',
      displayName: 'Netflix',
      avgAmount: 15.99,
      txnCount: 4,
      suspectedSubscription: true,
      cadence: 'monthly',
      confidence: 0.92,
      monthlyEquivalent: 15.99,
    },
  ],
  summary: {
    count: 1,
    monthlyTotal: 15.99,
    annualizedTotal: 191.88,
    optimizableAmount: 0,
  },
}

describe('merchantReports utils', () => {
  it('builds subscription KPIs', () => {
    const kpis = buildSubscriptionKpis(subscriptionReport)
    expect(kpis.find((k) => k.key === 'count')?.value).toBe('1')
    expect(kpis.find((k) => k.key === 'monthly')?.value).toContain('15.99')
  })

  it('builds subscription insights', () => {
    const insights = buildSubscriptionInsights(subscriptionReport)
    expect(insights[0].text).toContain('subscription')
  })

  it('builds concentration chart series', () => {
    const report: MerchantConcentrationReport = {
      totalSpend: 1000,
      merchantCount: 2,
      top3SharePct: 80,
      merchants: [
        { merchantToken: 'a', displayName: 'A', totalSpend: 600, sharePct: 60, txnCount: 3, suspectedSubscription: false },
        { merchantToken: 'b', displayName: 'B', totalSpend: 400, sharePct: 40, txnCount: 2, suspectedSubscription: true },
      ],
    }
    const option = buildConcentrationChart(report)
    const series = option.series as { data?: number[] }[]
    expect(series[0].data?.length).toBe(2)
  })

  it('builds drift comparison chart', () => {
    const report: MerchantDriftReport = {
      year: 2026,
      priorYear: 2025,
      movers: [
        { merchantToken: 'uber', displayName: 'Uber', currentSpend: 300, priorSpend: 100, deltaAmount: 200, pctChange: 200 },
      ],
    }
    const option = buildDriftChart(report)
    const series = option.series as { name?: string }[]
    expect(series.length).toBe(2)
  })
})
