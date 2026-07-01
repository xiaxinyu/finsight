import type { EChartsOption } from 'echarts'
import { REPORT_METRIC_HINTS } from '../components/MetricExplanation'
import { formatMoney } from './format'

export type TrendDeltaMetric = {
  from: number
  to: number
  deltaAmount: number
  deltaPercent: number
}

export type TrendMover = {
  categoryCode?: string
  categoryName?: string
  merchantToken?: string
  label?: string
  key?: string
  fromAmount?: number
  toAmount?: number
  pctChange?: number
  deltaAmount: number
  deltaPercent?: number
  contributionPct: number
  drillDown?: Record<string, string>
}

export type TrendItem = {
  type: string
  label: string
  fromAmount?: number
  toAmount?: number
  deltaAmount: number
  deltaPercent: number
  contributionPct: number
  drillDown?: Record<string, string>
}

export type TrendChangesReport = {
  fromYear: number
  toYear: number
  summary: {
    income: TrendDeltaMetric
    expense: TrendDeltaMetric
    savingsRate: TrendDeltaMetric
    fixedCost: TrendDeltaMetric
    headline?: string
  }
  topCategoryGrowth: TrendMover[]
  topMerchantMovers: TrendMover[]
  categoryYearMatrix?: CategoryYearMatrix
  categoryL1YearMatrix?: CategoryYearMatrix
  consumptionYearSeries?: ConsumptionYearPoint[]
  compareMode?: 'ytd_aligned' | 'full_year'
  historyFromYear?: number
  lifestyleInflation: {
    detected: boolean
    incomePctChange: number
    expensePctChange: number
    gapPct: number
    note: string
  }
  trends: TrendItem[]
}

export type CategoryYearMatrixRow = {
  tagId: string
  label: string
  amountsByYear: Record<string, number>
  shareByYear?: Record<string, number>
  deltaAmount: number
  deltaPercent: number
  yoyPercent?: number
  drillDown?: Record<string, string>
}

export type CategoryYearMatrix = {
  years: number[]
  partialYears?: string[]
  rows: CategoryYearMatrixRow[]
}

export type ConsumptionYearPoint = {
  year: number
  amount: number
  partial: boolean
  throughDate?: string
}

export function buildTrendKpis(report: TrendChangesReport) {
  const exp = report.summary.expense
  const sav = report.summary.savingsRate
  const life = report.lifestyleInflation
  return [
    {
      key: 'exp',
      label: 'Consumption Δ',
      value: formatMoney(exp.deltaAmount),
      tone: exp.deltaAmount > 0 ? 'expense' as const : 'income' as const,
      hint: `${report.fromYear}→${report.toYear}`,
      explain: REPORT_METRIC_HINTS.trendExpenseDelta,
    },
    {
      key: 'sav',
      label: 'Savings rate Δ',
      value: `${sav.deltaAmount >= 0 ? '+' : ''}${sav.deltaAmount.toFixed(1)} pts`,
      tone: sav.deltaAmount < 0 ? 'warn' as const : 'income' as const,
      explain: REPORT_METRIC_HINTS.savingsRateDelta,
    },
    {
      key: 'incGrowth',
      label: 'Income growth',
      value: `${life.incomePctChange >= 0 ? '+' : ''}${life.incomePctChange.toFixed(1)}%`,
      tone: life.incomePctChange >= 0 ? 'income' as const : 'warn' as const,
    },
    {
      key: 'expGrowth',
      label: 'Consumption growth',
      value: `${life.expensePctChange >= 0 ? '+' : ''}${life.expensePctChange.toFixed(1)}%`,
      tone: life.expensePctChange > life.incomePctChange ? 'expense' as const : 'neutral' as const,
    },
    {
      key: 'lifeGap',
      label: 'Growth gap',
      value: `${life.gapPct >= 0 ? '+' : ''}${life.gapPct.toFixed(1)} pts`,
      tone: life.detected ? 'warn' as const : 'neutral' as const,
      hint: life.detected ? 'Lifestyle inflation' : undefined,
    },
  ]
}

export type TrendYoYCard = {
  key: string
  label: string
  from: number
  to: number
  deltaAmount: number
  deltaPercent: number
  format: 'money' | 'percent'
  tone: 'income' | 'expense' | 'warn' | 'neutral'
  trendType?: string
}

export function buildTrendYoYCards(report: TrendChangesReport): TrendYoYCard[] {
  const inc = report.summary.income
  const exp = report.summary.expense
  const sav = report.summary.savingsRate
  return [
    {
      key: 'income',
      label: 'Income',
      from: inc.from,
      to: inc.to,
      deltaAmount: inc.deltaAmount,
      deltaPercent: inc.deltaPercent,
      format: 'money',
      tone: inc.deltaAmount >= 0 ? 'income' : 'warn',
      trendType: 'income_yoy',
    },
    {
      key: 'expense',
      label: 'Consumption',
      from: exp.from,
      to: exp.to,
      deltaAmount: exp.deltaAmount,
      deltaPercent: exp.deltaPercent,
      format: 'money',
      tone: exp.deltaAmount > 0 ? 'expense' : 'income',
      trendType: 'expense_yoy',
    },
    {
      key: 'savings',
      label: 'Savings rate',
      from: sav.from,
      to: sav.to,
      deltaAmount: sav.deltaAmount,
      deltaPercent: sav.deltaPercent,
      format: 'percent',
      tone: sav.deltaAmount >= 0 ? 'income' : 'warn',
      trendType: 'savings_rate',
    },
  ]
}

export function moverLabel(row: TrendMover): string {
  return String(row.label || row.categoryName || row.categoryCode || '—')
}

export function moverKey(row: TrendMover, kind: 'category' | 'merchant'): string {
  if (kind === 'category') return String(row.categoryCode || row.categoryName || row.label)
  return String(row.merchantToken || row.key || row.label)
}

export function trendChartHeight(count: number): number {
  const rows = Math.max(count, 3)
  return Math.min(420, Math.max(260, rows * 36 + 48))
}

export function findTrendItem(report: TrendChangesReport, type: string): TrendItem | undefined {
  return report.trends.find((t) => t.type === type)
}

export function buildTrendInsights(report: TrendChangesReport) {
  const bullets: { text: string; warn?: boolean }[] = [
    { text: report.summary.headline || 'Year-over-year spending shift.' },
  ]
  const life = report.lifestyleInflation
  bullets.push({
    text: `Income ${life.incomePctChange >= 0 ? '+' : ''}${life.incomePctChange.toFixed(1)}% vs expense ${life.expensePctChange >= 0 ? '+' : ''}${life.expensePctChange.toFixed(1)}% (gap ${life.gapPct >= 0 ? '+' : ''}${life.gapPct.toFixed(1)} pts).`,
    warn: life.detected,
  })
  if (life.detected) {
    bullets.push({ text: life.note, warn: true })
  }
  const topCat = report.topCategoryGrowth[0]
  const topMer = report.topMerchantMovers[0]
  if (topCat) {
    bullets.push({
      text: `Top category: ${topCat.categoryName} (${formatMoney(topCat.deltaAmount)}, ${Number(topCat.contributionPct).toFixed(0)}% of expense shift).`,
      warn: Number(topCat.deltaAmount) > 0,
    })
  }
  if (topMer) {
    bullets.push({
      text: `Top merchant: ${topMer.label} (${formatMoney(topMer.deltaAmount)}, ${Number(topMer.contributionPct).toFixed(0)}% of expense shift).`,
      warn: Number(topMer.deltaAmount) > 0,
    })
  }
  return bullets
}

function signedMoverChart(movers: TrendMover[], accent: string, limit = 8): EChartsOption {
  const sorted = [...movers]
    .sort((a, b) => Math.abs(Number(b.deltaAmount || 0)) - Math.abs(Number(a.deltaAmount || 0)))
    .slice(0, limit)
  const chartRows = [...sorted].reverse()
  const labels = chartRows.map((m) => moverLabel(m))

  return {
    grid: { left: 8, right: 20, top: 8, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: unknown) => {
        const row = Array.isArray(params) ? params[0] : params
        const idx = Number((row as { dataIndex?: number }).dataIndex ?? 0)
        const mover = chartRows[idx]
        if (!mover) return ''
        const from = mover.fromAmount
        const to = mover.toAmount
        const pct = Number(mover.deltaPercent ?? mover.pctChange ?? 0)
        const lines = [
          `<b>${moverLabel(mover)}</b>`,
          `Δ ${formatMoney(Number(mover.deltaAmount))} (${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%)`,
          `${Number(mover.contributionPct).toFixed(1)}% of expense shift`,
        ]
        if (from != null && to != null) {
          lines.splice(1, 0, `${formatMoney(from)} → ${formatMoney(to)}`)
        }
        return lines.join('<br/>')
      },
    },
    xAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 10,
        formatter: (v: number) => {
          const n = Math.abs(Number(v))
          if (n >= 10000) return `${(n / 10000).toFixed(0)}w`
          if (n >= 1000) return `${(n / 1000).toFixed(0)}k`
          return String(v)
        },
      },
      splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
    },
    yAxis: {
      type: 'category',
      data: labels,
      axisLabel: { interval: 0, fontSize: 11 },
    },
    series: [{
      name: 'YoY delta',
      type: 'bar',
      data: chartRows.map((m) => {
        const delta = Number(m.deltaAmount || 0)
        return {
          value: delta,
          name: moverLabel(m),
          itemStyle: { color: delta >= 0 ? accent : '#16a34a', borderRadius: delta >= 0 ? [0, 4, 4, 0] : [4, 0, 0, 4] },
        }
      }),
      barMaxWidth: 22,
    }],
  }
}

export function buildCategoryContributorChart(report: TrendChangesReport): EChartsOption {
  return signedMoverChart(report.topCategoryGrowth, '#7c3aed')
}

export function buildMerchantContributorChart(report: TrendChangesReport): EChartsOption {
  return signedMoverChart(report.topMerchantMovers, '#2563eb')
}

/** @deprecated use buildCategoryContributorChart / buildMerchantContributorChart */
export function buildContributorChart(report: TrendChangesReport): EChartsOption {
  return buildCategoryContributorChart(report)
}

export function trendTypeLabel(type: string): string {
  switch (type) {
    case 'income_yoy': return 'Income YoY'
    case 'expense_yoy': return 'Expense YoY'
    case 'savings_rate': return 'Savings rate'
    case 'fixed_cost': return 'Fixed costs'
    case 'category_mover': return 'Category'
    case 'merchant_mover': return 'Merchant'
    case 'lifestyle_inflation': return 'Lifestyle inflation'
    default: return type
  }
}

export function moverFromTo(row: TrendMover | TrendItem, summary?: TrendDeltaMetric): { from: number | null; to: number | null } {
  if ('fromAmount' in row && row.fromAmount != null && row.toAmount != null) {
    return { from: row.fromAmount, to: row.toAmount }
  }
  if (summary) {
    return { from: summary.from, to: summary.to }
  }
  return { from: null, to: null }
}

/** Total consumption by calendar year (official semantic totals). */
export function buildConsumptionTotalYearChart(series: ConsumptionYearPoint[]): EChartsOption {
  const labels = series.map((p) => (p.partial ? `${p.year} YTD` : String(p.year)))
  const amounts = series.map((p) => p.amount)
  const yoy = series.map((p, i) => {
    if (i === 0) return null
    const prev = series[i - 1].amount
    return prev > 0 ? ((p.amount - prev) / prev) * 100 : null
  })
  return {
    grid: { left: 48, right: 48, top: 36, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const rows = Array.isArray(params) ? params : [params]
        const idx = Number((rows[0] as { dataIndex?: number }).dataIndex ?? 0)
        const pt = series[idx]
        if (!pt) return ''
        const lines = [`<b>${labels[idx]}</b>`, `Consumption ${formatMoney(pt.amount)}`]
        const pct = yoy[idx]
        if (pct != null) {
          lines.push(`YoY ${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%`)
        }
        if (pt.partial && pt.throughDate) {
          lines.push(`Through ${pt.throughDate}`)
        }
        return lines.join('<br/>')
      },
    },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [{
      name: 'Consumption',
      type: 'bar',
      data: amounts.map((v, i) => ({
        value: v,
        itemStyle: {
          color: series[i].partial ? '#94a3b8' : '#2563eb',
          borderRadius: [4, 4, 0, 0],
        },
      })),
      barMaxWidth: 48,
    }],
  }
}

/** Stacked bar: top classifications per year. */
export function buildCategoryYearTrendChart(matrix: CategoryYearMatrix, topN = 6): EChartsOption {
  const years = matrix.years.map(String)
  const rows = matrix.rows.slice(0, topN)
  const palette = ['#2563eb', '#7c3aed', '#059669', '#d97706', '#64748b', '#dc2626']
  return {
    grid: { left: 48, right: 16, top: 40, bottom: 28 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: rows.map((r) => r.label), top: 4, type: 'scroll', textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: years, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: rows.map((row, i) => ({
      name: row.label,
      type: 'bar' as const,
      stack: 'consumption',
      data: years.map((y) => Number(row.amountsByYear[y] ?? 0)),
      itemStyle: { color: palette[i % palette.length] },
      barMaxWidth: 36,
    })),
  }
}

export function categoryYearMatrixTotals(matrix: CategoryYearMatrix): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const y of matrix.years) {
    totals[String(y)] = matrix.rows.reduce((s, r) => s + Number(r.amountsByYear[String(y)] ?? 0), 0)
  }
  return totals
}

export function officialConsumptionTotals(series: ConsumptionYearPoint[]): Record<string, number> {
  const totals: Record<string, number> = {}
  for (const p of series) {
    totals[String(p.year)] = p.amount
  }
  return totals
}

export function yearColumnLabel(year: number, partialYears?: string[]): string {
  return partialYears?.includes(String(year)) ? `${year} YTD` : String(year)
}

function csvEscape(value: string | number): string {
  const s = String(value)
  if (/[",\n]/.test(s)) return `"${s.replace(/"/g, '""')}"`
  return s
}

/** Export classification × year matrix for spreadsheet analysis. */
export function downloadCategoryYearMatrixCsv(
  matrix: CategoryYearMatrix,
  officialTotals: Record<string, number>,
  filename: string,
): void {
  const partial = matrix.partialYears
  const headers = [
    'Bucket',
    ...matrix.years.map((y) => yearColumnLabel(y, partial)),
    'YoY %',
    `Δ ${matrix.years[0]}→${matrix.years[matrix.years.length - 1]}`,
  ]
  const rows = matrix.rows.map((row) => [
    row.label,
    ...matrix.years.map((y) => String(row.amountsByYear[String(y)] ?? 0)),
    row.yoyPercent != null ? row.yoyPercent.toFixed(1) : '',
    String(row.deltaAmount),
  ].map(csvEscape).join(','))
  const totalRow = [
    'Total',
    ...matrix.years.map((y) => String(officialTotals[String(y)] ?? 0)),
    '',
    '',
  ].map(csvEscape).join(',')
  const blob = new Blob([[headers.map(csvEscape).join(','), ...rows, totalRow].join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
