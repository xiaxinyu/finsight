import type { EChartsOption } from 'echarts'
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
  pctChange?: number
  deltaAmount: number
  deltaPercent?: number
  contributionPct: number
  drillDown?: Record<string, string>
}

export type TrendItem = {
  type: string
  label: string
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
  return [
    { key: 'from', label: 'From', value: String(report.fromYear) },
    { key: 'to', label: 'To', value: String(report.toYear) },
    {
      key: 'exp',
      label: 'Expense Δ',
      value: formatMoney(exp.deltaAmount),
      tone: exp.deltaAmount > 0 ? 'expense' as const : 'income' as const,
    },
    {
      key: 'sav',
      label: 'Savings rate Δ',
      value: `${sav.deltaAmount >= 0 ? '+' : ''}${sav.deltaAmount.toFixed(1)} pts`,
      tone: sav.deltaAmount < 0 ? 'warn' as const : 'income' as const,
    },
    {
      key: 'life',
      label: 'Lifestyle inflation',
      value: report.lifestyleInflation.detected ? 'Detected' : 'None',
      tone: report.lifestyleInflation.detected ? 'warn' as const : 'neutral' as const,
    },
  ]
}

export function buildTrendInsights(report: TrendChangesReport) {
  const bullets: { text: string; warn?: boolean }[] = [
    { text: report.summary.headline || 'Year-over-year spending shift.' },
  ]
  if (report.lifestyleInflation.detected) {
    bullets.push({ text: report.lifestyleInflation.note, warn: true })
  }
  const top = report.topMerchantMovers[0] || report.topCategoryGrowth[0]
  if (top) {
    const label = String(top.label || top.categoryName || top.categoryCode)
    bullets.push({
      text: `Largest mover: ${label} (${formatMoney(top.deltaAmount)}, ${top.contributionPct?.toFixed?.(0) ?? top.contributionPct}% of expense change).`,
      warn: Number(top.deltaAmount) > 0,
    })
  }
  return bullets
}

export function buildContributorChart(report: TrendChangesReport): EChartsOption {
  const categories = report.topCategoryGrowth.slice(0, 6)
  const merchants = report.topMerchantMovers.slice(0, 4)
  const labels = [
    ...categories.map((c) => String(c.categoryName || c.categoryCode)),
    ...merchants.map((m) => String(m.label)),
  ]
  const values = [
    ...categories.map((c) => Number(c.contributionPct || 0)),
    ...merchants.map((m) => Number(m.contributionPct || 0)),
  ]
  return {
    grid: { left: 48, right: 16, top: 48, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 10, rotate: 25 } },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series: [{
      name: 'Contribution',
      type: 'bar',
      data: values,
      itemStyle: { color: '#7c3aed' },
      barMaxWidth: 28,
    }],
  }
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
