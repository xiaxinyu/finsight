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
  lifestyleInflation: {
    detected: boolean
    incomePctChange: number
    expensePctChange: number
    gapPct: number
    note: string
  }
  trends: TrendItem[]
}

export function buildTrendKpis(report: TrendChangesReport) {
  const exp = report.summary.expense
  const sav = report.summary.savingsRate
  const life = report.lifestyleInflation
  return [
    {
      key: 'exp',
      label: 'Expense Δ',
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
      label: 'Expense growth',
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
      label: 'Expense',
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
