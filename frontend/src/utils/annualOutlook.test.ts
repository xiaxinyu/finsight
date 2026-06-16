import { describe, expect, it } from 'vitest'
import type { ForecastData } from '../api/analytics'
import {
  buildAnnualOutlookChartOption,
  buildAnnualOutlookInsights,
  buildAnnualOutlookKpis,
  buildCategoryForecastChartOption,
  deficitRowClassName,
  isDeficitMonth,
  scenarioLabel,
} from './annualOutlook'

const sampleForecast: ForecastData = {
  year: 2026,
  scenario: 'stress',
  runId: 'run-1',
  yearIncome: 96000,
  yearExpense: 102000,
  yearNet: -6000,
  yearNetLower: -6900,
  yearNetUpper: -5100,
  deficitMonths: ['2026-03', '2026-08'],
  confidence: { halfWidthPct: 15, method: 'scenario_scaled_rolling_mean' },
  categoryForecasts: [
    {
      categoryCode: 'food',
      categoryName: 'Food',
      yearTotal: 18000,
      yearTotalLower: 15300,
      yearTotalUpper: 20700,
      sharePct: 17.6,
      months: [
        { yearMonth: '2026-01', amount: 1500, amountLower: 1275, amountUpper: 1725 },
        { yearMonth: '2026-03', amount: 1600, amountLower: 1360, amountUpper: 1840 },
      ],
    },
  ],
  months: [
    {
      yearMonth: '2026-01',
      income: 8000,
      expense: 7500,
      net: 500,
      netLower: 450,
      netUpper: 550,
    },
    {
      yearMonth: '2026-03',
      income: 7000,
      expense: 9000,
      net: -2000,
      netLower: -2200,
      netUpper: -1800,
    },
  ],
  budgetSuggestion: {
    monthlyCap: 9350,
    annualCap: 112200,
    note: 'Suggested cap from stress forecast with cushion.',
  },
}

describe('annualOutlook utils', () => {
  it('labels scenarios', () => {
    expect(scenarioLabel('conservative')).toBe('Conservative')
    expect(scenarioLabel('unknown')).toBe('unknown')
  })

  it('detects deficit months', () => {
    expect(isDeficitMonth(sampleForecast, '2026-03')).toBe(true)
    expect(isDeficitMonth(sampleForecast, '2026-01')).toBe(false)
  })

  it('builds insights with deficit warning and budget note', () => {
    const insights = buildAnnualOutlookInsights(sampleForecast)
    expect(insights[0].warn).toBe(true)
    expect(insights[0].text).toContain('2026-03')
    expect(insights[1].text).toContain('Budget suggestion')
  })

  it('builds KPI strip including scenario and confidence', () => {
    const kpis = buildAnnualOutlookKpis(sampleForecast)
    expect(kpis.find((k) => k.key === 'scenario')?.value).toBe('Stress')
    expect(kpis.find((k) => k.key === 'conf')?.value).toBe('±15%')
    expect(kpis.find((k) => k.key === 'def')?.value).toBe('2')
  })

  it('builds chart with scenario-scaled confidence band label', () => {
    const option = buildAnnualOutlookChartOption(sampleForecast)
    const series = option.series as { name?: string; data?: unknown[]; markPoint?: { data?: unknown[] } }[]
    expect(series.some((s) => s.name === 'Net band')).toBe(true)
    expect(option.legend).toMatchObject({ data: expect.arrayContaining(['Net ±15%']) })
    const net = series.find((s) => s.name === 'Net')
    expect(net?.markPoint?.data?.length).toBe(2)
    const xAxis = option.xAxis as { axisLabel?: { formatter?: (v: string) => string } }
    expect(xAxis.axisLabel?.formatter?.('2026-03')).toContain('2026-03')
  })

  it('builds category forecast chart for top categories', () => {
    const option = buildCategoryForecastChartOption(
      sampleForecast.categoryForecasts ?? [],
      ['2026-01', '2026-03'],
    )
    const series = option.series as { name?: string }[]
    expect(series.some((s) => s.name === 'Food')).toBe(true)
    expect(series.some((s) => s.name === 'Food band')).toBe(true)
  })

  it('applies deficit row class', () => {
    expect(deficitRowClassName({ yearMonth: '2026-03' }, sampleForecast.deficitMonths)).toContain('deficit')
    expect(deficitRowClassName({ yearMonth: '2026-01' }, sampleForecast.deficitMonths)).not.toContain('deficit')
  })
})
