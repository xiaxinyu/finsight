import type { PeriodMetricSummary } from '../api/analytics'
import { REPORT_METRICS_SOURCE } from './reportTaxonomy'

export type DashboardPeriodTotals = {
  realIncome: number
  consumptionExpense: number
  net: number
  metricsSource: string
  months: { month: string; realIncome: number; consumptionExpense: number; net: number }[]
}

/** Map semantic period summary to dashboard totals (single source of truth). */
export function mapDashboardPeriodTotals(
  semantic: PeriodMetricSummary | undefined,
): DashboardPeriodTotals {
  return {
    realIncome: semantic?.realIncome ?? 0,
    consumptionExpense: semantic?.consumptionExpense ?? 0,
    net: semantic?.netCashflow ?? 0,
    metricsSource: semantic?.metricsSource ?? REPORT_METRICS_SOURCE,
    months: (semantic?.months ?? []).map((m) => ({
      month: m.month,
      realIncome: m.realIncome,
      consumptionExpense: m.consumptionExpense,
      net: m.net,
    })),
  }
}

/** @deprecated use {@link mapDashboardPeriodTotals} */
export function mergeDashboardPeriodTotals(
  semantic: PeriodMetricSummary | undefined,
  _report?: unknown,
): DashboardPeriodTotals & { usedSemantic: boolean } {
  const mapped = mapDashboardPeriodTotals(semantic)
  return { ...mapped, usedSemantic: true }
}
