import { describe, expect, it } from 'vitest'
import {
  buildConcentrationChart,
  buildConcentrationKpis,
  buildDriftChart,
  buildSubscriptionInsights,
  buildSubscriptionKpis,
  formatStability,
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
      lastSeen: '2026-04-04',
      suspectedSubscription: true,
      cadence: 'monthly',
      confidence: 0.92,
      avgIntervalDays: 30,
      amountCv: 0.02,
      evidence: '4 charges every ~30 days',
      monthlyEquivalent: 15.99,
      drillDown: { merchantToken: 'netflix' },
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

  it('formats stability from interval and CV', () => {
    expect(formatStability(subscriptionReport.subscriptions[0])).toContain('30d')
    expect(formatStability(subscriptionReport.subscriptions[0])).toContain('CV')
  })

  it('builds concentration KPIs with top1 and top5', () => {
    const report: MerchantConcentrationReport = {
      totalSpend: 1000,
      merchantCount: 2,
      top1SharePct: 60,
      top3SharePct: 100,
      top5SharePct: 100,
      merchants: [
        { merchantToken: 'a', displayName: 'A', totalSpend: 600, sharePct: 60, txnCount: 3, suspectedSubscription: false },
        { merchantToken: 'b', displayName: 'B', totalSpend: 400, sharePct: 40, txnCount: 2, suspectedSubscription: true },
      ],
    }
    const kpis = buildConcentrationKpis(report)
    expect(kpis.find((k) => k.key === 'top1')?.value).toBe('60.0%')
    expect(kpis.find((k) => k.key === 'top5')?.value).toBe('100.0%')
    const option = buildConcentrationChart(report)
    const series = option.series as { data?: number[] }[]
    expect(series[0].data?.length).toBe(2)
  })

  it('builds drift comparison chart and supports buckets', () => {
    const report: MerchantDriftReport = {
      year: 2026,
      priorYear: 2025,
      movers: [
        { merchantToken: 'uber', displayName: 'Uber', currentSpend: 300, priorSpend: 100, deltaAmount: 200, pctChange: 200 },
      ],
      newMerchants: [],
      growingMerchants: [],
      decliningMerchants: [],
    }
    const option = buildDriftChart(report)
    const series = option.series as { name?: string }[]
    expect(series.length).toBe(2)
  })
})
