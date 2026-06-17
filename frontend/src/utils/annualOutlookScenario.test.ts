import { describe, expect, it } from 'vitest'
import type { ForecastData, ForecastMonth } from '../api/analytics'
import {
  EMPTY_SCENARIO_INPUTS,
  buildDeficitMonthGuidance,
  buildMethodologyBullets,
  countActualMonths,
  hasActiveScenarioInputs,
  monthActualNet,
  monthForecastNet,
  scenarioInputsToApi,
} from './annualOutlookScenario'

const month: ForecastMonth = {
  yearMonth: '2026-03',
  income: 7000,
  expense: 9000,
  net: -2000,
  forecast: true,
  budgetTarget: 8000,
}

const forecast: ForecastData = {
  year: 2026,
  scenario: 'stress',
  runId: 'r1',
  yearIncome: 90000,
  yearExpense: 100000,
  yearNet: -10000,
  deficitMonths: ['2026-03'],
  months: [month, { yearMonth: '2026-01', income: 8000, expense: 7000, net: 1000, actual: true }],
  confidence: { halfWidthPct: 15, method: 'scenario_scaled_rolling_mean' },
  explanation: ['Rolling 6-month average with seasonal index over the last 24 months of history.'],
  adjustments: {
    incomeChangePct: -5,
    newMonthlyBill: 400,
  },
}

describe('annualOutlookScenario', () => {
  it('detects active scenario inputs', () => {
    expect(hasActiveScenarioInputs(EMPTY_SCENARIO_INPUTS)).toBe(false)
    expect(hasActiveScenarioInputs({ ...EMPTY_SCENARIO_INPUTS, incomeChangePct: -8 })).toBe(true)
  })

  it('maps inputs to API payload omitting zeros', () => {
    expect(scenarioInputsToApi({
      incomeChangePct: -10,
      lumpSumExpense: 0,
      newMonthlyBill: 500,
      targetMonthlyPayment: null,
    })).toEqual({ incomeChangePct: -10, newMonthlyBill: 500 })
  })

  it('counts actual months', () => {
    expect(countActualMonths(forecast.months)).toBe(1)
  })

  it('splits actual vs forecast net', () => {
    expect(monthActualNet(month)).toBeNull()
    expect(monthForecastNet(month)).toBe(-2000)
    expect(monthActualNet(forecast.months![1])).toBe(1000)
  })

  it('builds methodology bullets with history and confidence', () => {
    const bullets = buildMethodologyBullets(forecast)
    expect(bullets.some((b) => b.text.includes('History window'))).toBe(true)
    expect(bullets.some((b) => b.text.includes('Confidence interval'))).toBe(true)
    expect(bullets.some((b) => b.text.includes('income change'))).toBe(true)
  })

  it('builds deficit month guidance with reasons and actions', () => {
    const { reasons, actions } = buildDeficitMonthGuidance(month, forecast)
    expect(reasons[0]).toContain('Projected net')
    expect(reasons.some((r) => r.includes('budget'))).toBe(true)
    expect(actions.some((a) => a.path === '/planning')).toBe(true)
  })
})
