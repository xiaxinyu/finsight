import type { EChartsOption } from 'echarts'
import type { CategoryYearMatrix, CategoryYearMatrixRow } from './trendChanges'
import { buildCategoryYearTrendChart, downloadCategoryYearMatrixCsv, yearColumnLabel } from './trendChanges'
import { formatMoney } from './format'
import { SEMANTIC_TAG_LABELS } from './categorySemantics'

export type TrendDeltaMetric = {
  from: number
  to: number
  deltaAmount: number
  deltaPercent: number
}

export type DebtTrendMover = {
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

export type DebtYearPoint = {
  year: number
  borrowing: number
  repayment: number
  net: number
  partial: boolean
  throughDate?: string
}

export type DebtTrendsReport = {
  fromYear: number
  toYear: number
  historyFromYear?: number
  compareMode?: 'ytd_aligned' | 'full_year'
  summary: {
    borrowing: TrendDeltaMetric
    repayment: TrendDeltaMetric
    netFlow: TrendDeltaMetric
    headline?: string
  }
  debtYearSeries: DebtYearPoint[]
  repaymentTypeMatrix: CategoryYearMatrix
  borrowingTypeMatrix: CategoryYearMatrix
  topRepaymentGrowth: DebtTrendMover[]
  debtPressure: {
    detected: boolean
    borrowingPctChange: number
    repaymentPctChange: number
    gapPct: number
    note: string
  }
}

export type DebtYoYCard = {
  key: string
  label: string
  from: number
  to: number
  deltaAmount: number
  deltaPercent: number
  tone: 'warn' | 'expense' | 'income' | 'neutral'
  hint?: string
}

const CARD_ORDER = ['repayment', 'borrowing', 'netFlow'] as const

export function debtTypeLabel(tagId: string, fallback?: string): string {
  return SEMANTIC_TAG_LABELS[tagId as keyof typeof SEMANTIC_TAG_LABELS] || fallback || tagId
}

export function buildDebtYoYCards(report: DebtTrendsReport): DebtYoYCard[] {
  const rep = report.summary.repayment
  const bor = report.summary.borrowing
  const net = report.summary.netFlow
  return [
    {
      key: 'repayment',
      label: 'Repayments',
      from: rep.from,
      to: rep.to,
      deltaAmount: rep.deltaAmount,
      deltaPercent: rep.deltaPercent,
      tone: rep.deltaAmount > 0 ? 'warn' : 'income',
      hint: 'Money paid toward loans & credit',
    },
    {
      key: 'borrowing',
      label: 'New borrowing',
      from: bor.from,
      to: bor.to,
      deltaAmount: bor.deltaAmount,
      deltaPercent: bor.deltaPercent,
      tone: bor.deltaAmount > 0 ? 'expense' : 'neutral',
      hint: 'Loans received & credit drawn',
    },
    {
      key: 'netFlow',
      label: 'Net flow',
      from: net.from,
      to: net.to,
      deltaAmount: net.deltaAmount,
      deltaPercent: net.deltaPercent,
      tone: net.to < 0 ? 'warn' : net.to > 0 ? 'income' : 'neutral',
      hint: 'Borrowing minus repayments (negative = debt load up)',
    },
  ]
}

export function orderedDebtCards(cards: DebtYoYCard[]): DebtYoYCard[] {
  return CARD_ORDER
    .map((key) => cards.find((c) => c.key === key))
    .filter((c): c is DebtYoYCard => c != null)
}

export function buildDebtInsights(report: DebtTrendsReport) {
  const bullets: { text: string; warn?: boolean }[] = [
    { text: report.summary.headline || 'Year-over-year debt cash flow.' },
  ]
  const pressure = report.debtPressure
  bullets.push({
    text: `Borrowing ${pressure.borrowingPctChange >= 0 ? '+' : ''}${pressure.borrowingPctChange.toFixed(1)}% vs repayments ${pressure.repaymentPctChange >= 0 ? '+' : ''}${pressure.repaymentPctChange.toFixed(1)}% (gap ${pressure.gapPct >= 0 ? '+' : ''}${pressure.gapPct.toFixed(1)} pts).`,
    warn: pressure.detected,
  })
  if (pressure.detected) {
    bullets.push({ text: pressure.note, warn: true })
  }
  const top = report.topRepaymentGrowth[0]
  if (top) {
    bullets.push({
      text: `Largest repayment shift: ${top.categoryName || top.label} (${formatMoney(top.deltaAmount)}).`,
      warn: Number(top.deltaAmount) > 0,
    })
  }
  return bullets
}

export function buildDebtYearChart(series: DebtYearPoint[]): EChartsOption {
  const labels = series.map((p) => yearColumnLabel(p.year, series.filter((x) => x.partial).map((x) => String(x.year))))
  return {
    grid: { left: 48, right: 16, top: 40, bottom: 28 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['Repayments', 'New borrowing'], top: 4, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [
      {
        name: 'Repayments',
        type: 'bar',
        data: series.map((p) => ({
          value: p.repayment,
          itemStyle: { color: p.partial ? '#fdba74' : '#ea580c', borderRadius: [4, 4, 0, 0] },
        })),
        barMaxWidth: 36,
      },
      {
        name: 'New borrowing',
        type: 'bar',
        data: series.map((p) => ({
          value: p.borrowing,
          itemStyle: { color: p.partial ? '#93c5fd' : '#2563eb', borderRadius: [4, 4, 0, 0] },
        })),
        barMaxWidth: 36,
      },
    ],
  }
}

export function buildNetDebtLineChart(series: DebtYearPoint[]): EChartsOption {
  const labels = series.map((p) => yearColumnLabel(p.year, series.filter((x) => x.partial).map((x) => String(x.year))))
  return {
    grid: { left: 48, right: 16, top: 24, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const row = Array.isArray(params) ? params[0] : params
        const idx = Number((row as { dataIndex?: number }).dataIndex ?? 0)
        const pt = series[idx]
        if (!pt) return ''
        return [
          `<b>${labels[idx]}</b>`,
          `Net ${formatMoney(pt.net)}`,
          `Borrowed ${formatMoney(pt.borrowing)} · Repaid ${formatMoney(pt.repayment)}`,
        ].join('<br/>')
      },
    },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [{
      name: 'Net flow',
      type: 'line',
      smooth: true,
      data: series.map((p) => p.net),
      itemStyle: { color: '#7c2d12' },
      areaStyle: { opacity: 0.08, color: '#ea580c' },
    }],
  }
}

export { buildCategoryYearTrendChart, downloadCategoryYearMatrixCsv, yearColumnLabel }

export function debtMatrixTotals(matrix: CategoryYearMatrix): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const y of matrix.years) {
    totals[String(y)] = matrix.rows.reduce((s, r) => s + Number(r.amountsByYear[String(y)] ?? 0), 0)
  }
  return totals
}

export function officialDebtTotals(series: DebtYearPoint[], kind: 'repayment' | 'borrowing'): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const p of series) {
    totals[String(p.year)] = kind === 'repayment' ? p.repayment : p.borrowing
  }
  return totals
}

export function buildRepaymentMoverChart(movers: DebtTrendMover[]): EChartsOption {
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
      name: 'Repayment Δ',
      type: 'bar',
      data: chartRows.map((m) => {
        const delta = Number(m.deltaAmount || 0)
        return {
          value: delta,
          itemStyle: { color: delta >= 0 ? '#ea580c' : '#16a34a', borderRadius: delta >= 0 ? [0, 4, 4, 0] : [4, 0, 0, 4] },
        }
      }),
      barMaxWidth: 22,
    }],
  }
}

export function moverLabel(row: DebtTrendMover): string {
  return String(row.label || row.categoryName || row.categoryCode || '—')
}

export function moverKey(row: DebtTrendMover): string {
  return String(row.categoryCode || row.categoryName || row.label)
}

export function trendChartHeight(count: number): number {
  const rows = Math.max(count, 3)
  return Math.min(420, Math.max(220, rows * 36 + 48))
}

export type { CategoryYearMatrix, CategoryYearMatrixRow }
