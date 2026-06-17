import type { ForecastData, ForecastMonth } from '../api/analytics'
import type { DrillDownAction } from '../components/drilldown/types'
import { formatMoney } from './format'
import { budgetGap } from './fsTableCells'
import { scenarioLabel } from './annualOutlook'

export type ScenarioInputsState = {
  incomeChangePct: number | null
  lumpSumExpense: number | null
  newMonthlyBill: number | null
  targetMonthlyPayment: number | null
}

export const EMPTY_SCENARIO_INPUTS: ScenarioInputsState = {
  incomeChangePct: null,
  lumpSumExpense: null,
  newMonthlyBill: null,
  targetMonthlyPayment: null,
}

export function hasActiveScenarioInputs(inputs: ScenarioInputsState): boolean {
  return [
    inputs.incomeChangePct,
    inputs.lumpSumExpense,
    inputs.newMonthlyBill,
    inputs.targetMonthlyPayment,
  ].some((v) => v != null && v !== 0)
}

export function scenarioInputsToApi(inputs: ScenarioInputsState) {
  const payload: Record<string, number> = {}
  if (inputs.incomeChangePct != null && inputs.incomeChangePct !== 0) {
    payload.incomeChangePct = inputs.incomeChangePct
  }
  if (inputs.lumpSumExpense != null && inputs.lumpSumExpense > 0) {
    payload.lumpSumExpense = inputs.lumpSumExpense
  }
  if (inputs.newMonthlyBill != null && inputs.newMonthlyBill > 0) {
    payload.newMonthlyBill = inputs.newMonthlyBill
  }
  if (inputs.targetMonthlyPayment != null && inputs.targetMonthlyPayment > 0) {
    payload.targetMonthlyPayment = inputs.targetMonthlyPayment
  }
  return payload
}

export function countActualMonths(months: ForecastMonth[] = []): number {
  return months.filter((m) => m.actual).length
}

export function buildMethodologyBullets(forecast: ForecastData): { text: string; warn?: boolean }[] {
  const bullets: { text: string; warn?: boolean }[] = []
  const actualCount = countActualMonths(forecast.months)
  bullets.push({
    text: `History window: rolling 24 months ending last completed month; ${actualCount} month(s) in ${forecast.year} use observed actuals.`,
  })
  if (forecast.confidence?.method) {
    bullets.push({
      text: `Confidence interval: ±${forecast.confidence.halfWidthPct ?? 10}% using ${forecast.confidence.method.replaceAll('_', ' ')}.`,
    })
  }
  for (const line of forecast.explanation || []) {
    if (!bullets.some((b) => b.text === line)) bullets.push({ text: line })
  }
  const adjustments = forecast.adjustments
  if (adjustments) {
    if (adjustments.incomeChangePct) {
      bullets.push({ text: `Applied income change: ${adjustments.incomeChangePct > 0 ? '+' : ''}${adjustments.incomeChangePct}%.` })
    }
    if (adjustments.newMonthlyBill) {
      bullets.push({ text: `Applied new monthly bill: ${formatMoney(adjustments.newMonthlyBill)}/mo.` })
    }
    if (adjustments.lumpSumExpense) {
      bullets.push({ text: `Applied lump-sum expense: ${formatMoney(adjustments.lumpSumExpense)} in January.` })
    }
    if (adjustments.targetMonthlyPayment) {
      bullets.push({ text: `Budget comparison uses target payment ${formatMoney(adjustments.targetMonthlyPayment)}/mo.` })
    }
  }
  return bullets
}

export function buildDeficitMonthGuidance(
  month: ForecastMonth,
  forecast: ForecastData,
): { reasons: string[]; actions: DrillDownAction[] } {
  const reasons: string[] = [
    `Projected net ${formatMoney(month.net)} (income ${formatMoney(month.income)}, expense ${formatMoney(month.expense)}).`,
  ]
  const gap = budgetGap(month.expense, month.budgetTarget)
  if (gap != null && gap < 0) {
    reasons.push(`Expense exceeds budget target by ${formatMoney(-gap)}.`)
  }
  if (month.expense > month.income * 1.2) {
    reasons.push('Expense is more than 20% above income this month.')
  }
  if (forecast.scenario === 'stress' || forecast.scenario === 'conservative') {
    reasons.push(`${scenarioLabel(forecast.scenario)} scenario amplifies downside pressure.`)
  }
  const actions: DrillDownAction[] = [
    { label: 'Open planning', type: 'planning', path: '/planning' },
    { label: 'Cash risk calendar', type: 'report', path: '/reports/cash-risk' },
    { label: 'Review expense transactions', type: 'transactions', path: '/transactions' },
  ]
  return { reasons, actions }
}

export function monthActualNet(month: ForecastMonth): number | null {
  return month.actual ? month.net : null
}

export function monthForecastNet(month: ForecastMonth): number | null {
  return month.forecast ? month.net : null
}
