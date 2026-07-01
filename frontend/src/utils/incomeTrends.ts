import type { EChartsOption } from 'echarts'
import type { CategoryYearMatrix, CategoryYearMatrixRow } from './trendChanges'
import {
  buildCategoryYearTrendChart,
  downloadCategoryYearMatrixCsv,
  yearColumnLabel,
} from './trendChanges'
import { formatMoney } from './format'

export type TrendDeltaMetric = {
  from: number
  to: number
  deltaAmount: number
  deltaPercent: number
}

export type IncomeTrendMover = {
  categoryCode?: string
  categoryName?: string
  label?: string
  fromAmount?: number
  toAmount?: number
  deltaAmount: number
  deltaPercent?: number
  pctChange?: number
  contributionPct: number
  drillDown?: Record<string, string>
}

export type IncomeYearPoint = {
  year: number
  amount: number
  partial: boolean
  throughDate?: string
}

export type IncomeTrendsReport = {
  fromYear: number
  toYear: number
  historyFromYear?: number
  compareMode?: 'ytd_aligned' | 'full_year'
  summary: {
    totalIncome: TrendDeltaMetric
    realIncome: TrendDeltaMetric
    investmentIncome: TrendDeltaMetric
    otherIncome: TrendDeltaMetric
    headline?: string
  }
  incomeYearSeries: IncomeYearPoint[]
  incomeTypeMatrix: CategoryYearMatrix
  categoryL1YearMatrix: CategoryYearMatrix
  topIncomeGrowth: IncomeTrendMover[]
  incomeMomentum: {
    detected: boolean
    totalPctChange: number
    realPctChange: number
    note: string
  }
}

export type IncomeYoYCard = {
  key: string
  label: string
  from: number
  to: number
  deltaAmount: number
  deltaPercent: number
  tone: 'income' | 'warn' | 'neutral'
  hint?: string
}

const CARD_ORDER = ['total', 'real', 'investment'] as const

export function buildIncomeYoYCards(report: IncomeTrendsReport): IncomeYoYCard[] {
  const total = report.summary.totalIncome
  const real = report.summary.realIncome
  const invest = report.summary.investmentIncome
  return [
    {
      key: 'total',
      label: 'Total income',
      from: total.from,
      to: total.to,
      deltaAmount: total.deltaAmount,
      deltaPercent: total.deltaPercent,
      tone: total.deltaAmount >= 0 ? 'income' : 'warn',
      hint: 'Salary, bonus, side income & investment returns',
    },
    {
      key: 'real',
      label: 'Real income',
      from: real.from,
      to: real.to,
      deltaAmount: real.deltaAmount,
      deltaPercent: real.deltaPercent,
      tone: real.deltaAmount >= 0 ? 'income' : 'warn',
      hint: 'Salary, wages, and regular pay',
    },
    {
      key: 'investment',
      label: 'Investment income',
      from: invest.from,
      to: invest.to,
      deltaAmount: invest.deltaAmount,
      deltaPercent: invest.deltaPercent,
      tone: invest.deltaAmount >= 0 ? 'income' : 'neutral',
      hint: 'Dividends, interest, and investment returns',
    },
  ]
}

export function orderedIncomeCards(cards: IncomeYoYCard[]): IncomeYoYCard[] {
  return CARD_ORDER
    .map((key) => cards.find((c) => c.key === key))
    .filter((c): c is IncomeYoYCard => c != null)
}

export function buildIncomeInsights(report: IncomeTrendsReport) {
  const bullets: { text: string; warn?: boolean }[] = [
    { text: report.summary.headline || 'Year-over-year income change.' },
  ]
  const mom = report.incomeMomentum
  bullets.push({
    text: `Total ${mom.totalPctChange >= 0 ? '+' : ''}${mom.totalPctChange.toFixed(1)}% · Real income ${mom.realPctChange >= 0 ? '+' : ''}${mom.realPctChange.toFixed(1)}%.`,
    warn: !mom.detected && report.summary.totalIncome.deltaAmount < 0,
  })
  bullets.push({ text: mom.note, warn: report.summary.totalIncome.deltaAmount < 0 })
  const top = report.topIncomeGrowth[0]
  if (top) {
    bullets.push({
      text: `Largest shift: ${top.categoryName || top.label} (${formatMoney(top.deltaAmount)}).`,
      warn: Number(top.deltaAmount) < 0,
    })
  }
  return bullets.slice(0, 3)
}

export function buildIncomeTotalYearChart(series: IncomeYearPoint[]): EChartsOption {
  const partialYears = series.filter((p) => p.partial).map((p) => String(p.year))
  const labels = series.map((p) => yearColumnLabel(p.year, partialYears))
  return {
    grid: { left: 48, right: 48, top: 36, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const row = Array.isArray(params) ? params[0] : params
        const idx = Number((row as { dataIndex?: number }).dataIndex ?? 0)
        const pt = series[idx]
        if (!pt) return ''
        return [`<b>${labels[idx]}</b>`, `Income ${formatMoney(pt.amount)}`].join('<br/>')
      },
    },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [{
      name: 'Income',
      type: 'bar',
      data: series.map((p) => ({
        value: p.amount,
        itemStyle: {
          color: p.partial ? '#86efac' : '#16a34a',
          borderRadius: [4, 4, 0, 0],
        },
      })),
      barMaxWidth: 48,
    }],
  }
}

export function buildIncomeMoverChart(movers: IncomeTrendMover[]): EChartsOption {
  const sorted = [...movers]
    .sort((a, b) => Math.abs(Number(b.deltaAmount || 0)) - Math.abs(Number(a.deltaAmount || 0)))
    .slice(0, 8)
  const chartRows = [...sorted].reverse()
  const labels = chartRows.map((m) => m.categoryName || m.label || '—')
  return {
    grid: { left: 8, right: 20, top: 8, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: labels, axisLabel: { interval: 0, fontSize: 11 } },
    series: [{
      name: 'Income Δ',
      type: 'bar',
      data: chartRows.map((m) => {
        const delta = Number(m.deltaAmount || 0)
        return {
          value: delta,
          itemStyle: { color: delta >= 0 ? '#16a34a' : '#ea580c', borderRadius: delta >= 0 ? [0, 4, 4, 0] : [4, 0, 0, 4] },
        }
      }),
      barMaxWidth: 22,
    }],
  }
}

export { buildCategoryYearTrendChart, downloadCategoryYearMatrixCsv, yearColumnLabel }

export function incomeMatrixTotals(matrix: CategoryYearMatrix): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const y of matrix.years) {
    totals[String(y)] = matrix.rows.reduce((s, r) => s + Number(r.amountsByYear[String(y)] ?? 0), 0)
  }
  return totals
}

export function officialIncomeTotals(series: IncomeYearPoint[]): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const p of series) {
    totals[String(p.year)] = p.amount
  }
  return totals
}

export function moverLabel(row: IncomeTrendMover): string {
  return String(row.label || row.categoryName || row.categoryCode || '—')
}

export function moverKey(row: IncomeTrendMover): string {
  return String(row.categoryCode || row.categoryName || row.label)
}

export function trendChartHeight(count: number): number {
  const rows = Math.max(count, 3)
  return Math.min(420, Math.max(220, rows * 36 + 48))
}

export type { CategoryYearMatrix, CategoryYearMatrixRow }
