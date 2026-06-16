import { describe, expect, it } from 'vitest'
import {
  buildContributorChart,
  buildTrendInsights,
  buildTrendKpis,
  type TrendChangesReport,
} from './trendChanges'

const sample: TrendChangesReport = {
  fromYear: 2025,
  toYear: 2026,
  summary: {
    income: { from: 100000, to: 110000, deltaAmount: 10000, deltaPercent: 10 },
    expense: { from: 80000, to: 95000, deltaAmount: 15000, deltaPercent: 18.75 },
    savingsRate: { from: 20, to: 13.6, deltaAmount: -6.4, deltaPercent: -6.4 },
    fixedCost: { from: 20000, to: 22000, deltaAmount: 2000, deltaPercent: 10 },
    headline: 'Spending is up ¥15,000 YoY — mainly Food and Uber.',
  },
  topCategoryGrowth: [
    { categoryCode: 'FOOD', categoryName: 'Food', deltaAmount: 8000, pctChange: 40, contributionPct: 53.3, drillDown: {} },
  ],
  topMerchantMovers: [
    { merchantToken: 'uber', label: 'Uber', deltaAmount: 3000, deltaPercent: 50, contributionPct: 20, drillDown: {} },
  ],
  lifestyleInflation: {
    detected: true,
    incomePctChange: 10,
    expensePctChange: 18.75,
    gapPct: 8.75,
    note: 'Spending grew faster than income.',
  },
  trends: [],
}

describe('trendChanges utils', () => {
  it('builds KPI strip', () => {
    const kpis = buildTrendKpis(sample)
    expect(kpis.find((k) => k.key === 'exp')?.value).toContain('15,000')
  })

  it('builds insights with headline and lifestyle warning', () => {
    const insights = buildTrendInsights(sample)
    expect(insights[0].text).toContain('Spending is up')
    expect(insights.some((b) => b.warn)).toBe(true)
  })

  it('builds contributor chart', () => {
    const option = buildContributorChart(sample)
    const series = option.series as { data?: number[] }[]
    expect(series[0].data?.length).toBe(2)
  })
})
