import { describe, expect, it } from 'vitest'
import {
  buildCategoryContributorChart,
  buildMerchantContributorChart,
  buildTrendInsights,
  buildTrendKpis,
  buildTrendYoYCards,
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
    { categoryCode: 'FOOD', categoryName: 'Food', fromAmount: 20000, toAmount: 28000, deltaAmount: 8000, pctChange: 40, deltaPercent: 40, contributionPct: 53.3, drillDown: {} },
  ],
  topMerchantMovers: [
    { merchantToken: 'uber', label: 'Uber', fromAmount: 6000, toAmount: 9000, deltaAmount: 3000, deltaPercent: 50, contributionPct: 20, drillDown: {} },
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
  it('builds KPI strip without redundant year labels', () => {
    const kpis = buildTrendKpis(sample)
    expect(kpis.find((k) => k.key === 'from')).toBeUndefined()
    expect(kpis.find((k) => k.key === 'exp')?.value).toContain('15,000')
    expect(kpis.find((k) => k.key === 'incGrowth')?.value).toContain('10.0%')
    expect(kpis.find((k) => k.key === 'lifeGap')?.value).toContain('8.8')
  })

  it('builds YoY summary cards for income expense and savings', () => {
    const cards = buildTrendYoYCards(sample)
    expect(cards).toHaveLength(3)
    expect(cards[0].label).toBe('Income')
    expect(cards[1].label).toBe('Expense')
    expect(cards[2].format).toBe('percent')
  })

  it('builds insights with separate category and merchant drivers', () => {
    const insights = buildTrendInsights(sample)
    expect(insights[0].text).toContain('Spending is up')
    expect(insights.some((b) => b.text.includes('Top category'))).toBe(true)
    expect(insights.some((b) => b.text.includes('Top merchant'))).toBe(true)
  })

  it('builds signed delta mover charts', () => {
    const cat = buildCategoryContributorChart(sample)
    const mer = buildMerchantContributorChart(sample)
    const catSeries = cat.series as { data?: { value?: number }[] }[]
    const merSeries = mer.series as { data?: { value?: number }[] }[]
    expect(catSeries[0].data?.[0]?.value).toBe(8000)
    expect(merSeries[0].data?.[0]?.value).toBe(3000)
  })
})
