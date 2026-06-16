import { describe, expect, it } from 'vitest'
import type { ForecastData } from '../api/analytics'
import {
  buildAnnualOutlookChartOption,
  buildAnnualOutlookInsights,
  buildAnnualOutlookKpis,
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
  deficitMonths: ['2026-03', '2026-08'],
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

  it('builds KPI strip including scenario', () => {
    const kpis = buildAnnualOutlookKpis(sampleForecast)
    expect(kpis.find((k) => k.key === 'scenario')?.value).toBe('Stress')
    expect(kpis.find((k) => k.key === 'def')?.value).toBe('2')
  })

  it('builds chart with confidence band and deficit markers', () => {
    const option = buildAnnualOutlookChartOption(sampleForecast)
    const series = option.series as { name?: string; data?: unknown[]; markPoint?: { data?: unknown[] } }[]
    expect(series.some((s) => s.name === 'Net band')).toBe(true)
    const net = series.find((s) => s.name === 'Net')
    expect(net?.markPoint?.data?.length).toBe(2)
    const xAxis = option.xAxis as { axisLabel?: { formatter?: (v: string) => string } }
    expect(xAxis.axisLabel?.formatter?.('2026-03')).toContain('2026-03')
  })

  it('applies deficit row class', () => {
    expect(deficitRowClassName({ yearMonth: '2026-03' }, sampleForecast.deficitMonths)).toContain('deficit')
    expect(deficitRowClassName({ yearMonth: '2026-01' }, sampleForecast.deficitMonths)).not.toContain('deficit')
  })
})
