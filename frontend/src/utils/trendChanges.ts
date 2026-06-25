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
    { key: 'from', label: 'From', value: String(report.fromYear) },
    { key: 'to', label: 'To', value: String(report.toYear) },
    {
      key: 'exp',
      label: 'Expense Δ',
      value: formatMoney(exp.deltaAmount),
      tone: exp.deltaAmount > 0 ? 'expense' as const : 'income' as const,
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
    },
  ]
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

function contributionBarOption(labels: string[], values: number[], color: string): EChartsOption {
  const ordered = labels.map((label, i) => ({ label, value: Number(values[i] || 0) }))
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v) => `${Number(v).toFixed(1)}%`,
    },
    xAxis: {
      type: 'value',
      axisLabel: { formatter: '{value}%' },
    },
    yAxis: {
      type: 'category',
      data: ordered.map((r) => r.label).reverse(),
      axisLabel: { interval: 0 },
    },
    series: [{
      name: 'Contribution',
      type: 'bar',
      data: ordered.map((r) => r.value).reverse(),
      itemStyle: { color },
      barMaxWidth: 22,
    }],
  }
}

export function buildCategoryContributorChart(report: TrendChangesReport): EChartsOption {
  const categories = report.topCategoryGrowth.slice(0, 8)
  return contributionBarOption(
    categories.map((c) => String(c.categoryName || c.categoryCode)),
    categories.map((c) => Number(c.contributionPct || 0)),
    '#7c3aed',
  )
}

export function buildMerchantContributorChart(report: TrendChangesReport): EChartsOption {
  const merchants = report.topMerchantMovers.slice(0, 8)
  return contributionBarOption(
    merchants.map((m) => String(m.label)),
    merchants.map((m) => Number(m.contributionPct || 0)),
    '#2563eb',
  )
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
