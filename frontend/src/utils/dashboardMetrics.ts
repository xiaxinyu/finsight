import type { PeriodMetricSummary } from '../api/analytics'

export type DashboardPeriodTotals = {
  realIncome: number
  consumptionExpense: number
  net: number
  metricsSource: string
  months: { month: string; realIncome: number; consumptionExpense: number; net: number }[]
  usedSemantic: boolean
}

/** Prefer semantic period summary; fall back to legacy report totals when semantic layer is empty. */
export function mergeDashboardPeriodTotals(
  semantic: PeriodMetricSummary | undefined,
  report: { income: number; expense: number; months: { month: string; income: number; expense: number }[] } | undefined,
): DashboardPeriodTotals {
  const semanticHasData = semantic
    && (semantic.realIncome > 0 || semantic.consumptionExpense > 0)
  if (semanticHasData && semantic) {
    return {
      realIncome: semantic.realIncome,
      consumptionExpense: semantic.consumptionExpense,
      net: semantic.netCashflow,
      metricsSource: semantic.metricsSource,
      months: (semantic.months || []).map((m) => ({
        month: m.month,
        realIncome: m.realIncome,
        consumptionExpense: m.consumptionExpense,
        net: m.net,
      })),
      usedSemantic: true,
    }
  }
  const income = report?.income ?? 0
  const expense = report?.expense ?? 0
  return {
    realIncome: income,
    consumptionExpense: expense,
    net: income - expense,
    metricsSource: 'transaction_report',
    months: (report?.months || []).map((m) => ({
      month: m.month,
      realIncome: m.income,
      consumptionExpense: m.expense,
      net: m.income - m.expense,
    })),
    usedSemantic: false,
  }
}
