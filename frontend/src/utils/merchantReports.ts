import type { EChartsOption } from 'echarts'
import { REPORT_METRIC_HINTS } from '../components/MetricExplanation'
import { formatMoney } from './format'

export type MerchantSubscription = {
  merchantToken: string
  displayName: string
  avgAmount: number
  txnCount: number
  lastSeen?: string
  suspectedSubscription: boolean
  cadence: string
  confidence: number
  avgIntervalDays?: number
  amountCv?: number
  evidence?: string
  monthlyEquivalent: number
  periodSpend?: number
  detectionSource?: 'pattern' | 'category'
  drillDown?: Record<string, string>
}

export type SubscriptionReport = {
  subscriptions: MerchantSubscription[]
  summary: {
    count: number
    monthlyTotal: number
    annualizedTotal: number
    optimizableAmount: number
    patternCount?: number
    categoryOnlyCount?: number
    categoryTxnCount?: number
    categoryTotalSpend?: number
    categoryMerchantCount?: number
    periodStart?: string
    periodEnd?: string
  }
}

export type MerchantConcentrationRow = {
  merchantToken: string
  displayName: string
  totalSpend: number
  sharePct: number
  txnCount: number
  suspectedSubscription: boolean
  drillDown?: Record<string, string>
}

export type MerchantConcentrationReport = {
  totalSpend: number
  merchantCount: number
  top1SharePct?: number
  top3SharePct: number
  top5SharePct?: number
  merchants: MerchantConcentrationRow[]
}

export type MerchantDriftRow = {
  merchantToken: string
  displayName: string
  currentSpend: number
  priorSpend: number
  deltaAmount: number
  pctChange: number | null
  drillDown?: Record<string, string>
}

export type MerchantDriftReport = {
  year: number
  priorYear: number
  movers: MerchantDriftRow[]
  newMerchants?: MerchantDriftRow[]
  growingMerchants?: MerchantDriftRow[]
  decliningMerchants?: MerchantDriftRow[]
}

export function buildSubscriptionKpis(report: SubscriptionReport) {
  const s = report.summary
  return [
    { key: 'count', label: 'Merchants', value: String(s.count), explain: REPORT_METRIC_HINTS.merchantSubscription },
    {
      key: 'pattern',
      label: 'Pattern detected',
      value: String(s.patternCount ?? 0),
      hint: 'Recurring cadence + stable amount',
    },
    {
      key: 'category',
      label: 'Category only',
      value: String(s.categoryOnlyCount ?? 0),
      hint: 'Tagged subscription category, no pattern match',
    },
    { key: 'monthly', label: 'Monthly eq.', value: formatMoney(s.monthlyTotal), tone: 'expense' as const, explain: REPORT_METRIC_HINTS.merchantSubscription },
    {
      key: 'ledger',
      label: 'Category ledger',
      value: `${s.categoryTxnCount ?? 0} txns · ${formatMoney(s.categoryTotalSpend ?? 0)}`,
      hint: `${s.categoryMerchantCount ?? 0} merchants in period`,
    },
    {
      key: 'opt',
      label: 'Optimizable / yr',
      value: formatMoney(s.optimizableAmount),
      tone: s.optimizableAmount > 0 ? 'warn' as const : 'neutral' as const,
    },
  ]
}

export function formatSubscriptionPeriodLabel(report: SubscriptionReport): string {
  const s = report.summary
  if (s.periodStart && s.periodEnd) {
    return `${s.periodStart} → ${s.periodEnd}`
  }
  return 'Current year'
}

export function buildConcentrationKpis(report: MerchantConcentrationReport) {
  return [
    { key: 'total', label: 'Total spend', value: formatMoney(report.totalSpend), tone: 'expense' as const, explain: REPORT_METRIC_HINTS.merchantSpend },
    { key: 'merchants', label: 'Merchants', value: String(report.merchantCount) },
    { key: 'top1', label: 'Top 1 share', value: `${(report.top1SharePct ?? 0).toFixed(1)}%`, explain: REPORT_METRIC_HINTS.merchantConcentration },
    { key: 'top5', label: 'Top 5 share', value: `${(report.top5SharePct ?? report.top3SharePct).toFixed(1)}%`, explain: REPORT_METRIC_HINTS.merchantConcentration },
  ]
}

export function buildConcentrationChart(report: MerchantConcentrationReport): EChartsOption {
  const top = report.merchants.slice(0, 10)
  const names = top.map((m) => m.displayName)
  const shares = top.map((m) => m.sharePct)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v) => `${Number(v).toFixed(1)}%`,
    },
    xAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: [...names].reverse() },
    series: [{
      name: 'Share',
      type: 'bar',
      data: [...shares].reverse(),
      itemStyle: { color: '#2563eb' },
      barMaxWidth: 22,
    }],
  }
}

export function buildDriftChart(report: MerchantDriftReport): EChartsOption {
  const movers = report.movers.slice(0, 12)
  const names = movers.map((m) => m.displayName)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (v) => formatMoney(Number(v)),
    },
    legend: { data: [String(report.priorYear), String(report.year)], top: 4 },
    grid: { left: 8, right: 16, top: 48, bottom: 8, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: [...names].reverse(), axisLabel: { interval: 0 } },
    series: [
      { name: String(report.priorYear), type: 'bar', data: [...movers.map((m) => m.priorSpend)].reverse(), itemStyle: { color: '#94a3b8' }, barMaxWidth: 14 },
      { name: String(report.year), type: 'bar', data: [...movers.map((m) => m.currentSpend)].reverse(), itemStyle: { color: '#2563eb' }, barMaxWidth: 14 },
    ],
  }
}

export function buildSubscriptionInsights(report: SubscriptionReport, periodLabel?: string) {
  const s = report.summary
  const period = periodLabel || formatSubscriptionPeriodLabel(report)
  const bullets = [
    {
      text: `Period: ${period}. Two detection paths — Pattern (≥3 charges, stable cadence/amount) and Category (ledger tagged 订阅/会员).`,
      warn: false,
    },
    {
      text: s.count
        ? `${s.count} merchant(s) · ${formatMoney(s.monthlyTotal)}/mo equivalent (${s.patternCount ?? 0} pattern, ${s.categoryOnlyCount ?? 0} category-only).`
        : 'No subscriptions in this period — widen the date range or tag transactions in a subscription category.',
      warn: s.count === 0,
    },
  ]
  if ((s.categoryTxnCount ?? 0) > 0) {
    const listed = s.count ?? 0
    const merchants = s.categoryMerchantCount ?? 0
    const gap = merchants - listed
    bullets.push({
      text: `Category ledger in period: ${s.categoryTxnCount} transactions · ${formatMoney(s.categoryTotalSpend ?? 0)} across ${merchants} merchants.`,
      warn: false,
    })
    if (gap > 0) {
      bullets.push({
        text: `${gap} subscription-category merchant(s) may be missing from the table (single charge or below pattern threshold) — review Transactions filtered by subscription category.`,
        warn: true,
      })
    }
  }
  if (s.optimizableAmount > 0) {
    bullets.push({
      text: `Up to ${formatMoney(s.optimizableAmount)}/year may be optimizable (lower-confidence recurring charges).`,
      warn: true,
    })
  }
  const lowConf = report.subscriptions.filter((sub) => sub.confidence < 0.75)
  if (lowConf.length) {
    bullets.push({
      text: `${lowConf.length} subscription(s) have lower confidence — review evidence before cancelling.`,
      warn: true,
    })
  }
  return bullets
}

export function formatStability(sub: MerchantSubscription): string {
  if (sub.detectionSource === 'category') {
    return sub.evidence || 'Tagged in subscription category'
  }
  const cv = sub.amountCv != null ? `${Math.round(sub.amountCv * 100)}% amount CV` : ''
  const interval = sub.avgIntervalDays ? `~${Math.round(sub.avgIntervalDays)}d interval` : ''
  return [interval, cv].filter(Boolean).join(' · ') || '—'
}

export function formatCadenceLabel(cadence: string): string {
  switch (cadence) {
    case 'category': return 'Category'
    case 'monthly': return 'Monthly'
    case 'quarterly': return 'Quarterly'
    case 'yearly': return 'Yearly'
    default: return cadence
  }
}

export function driftBucketLabel(bucket: 'new' | 'growing' | 'declining'): string {
  switch (bucket) {
    case 'new': return 'New merchants'
    case 'growing': return 'Growing merchants'
    case 'declining': return 'Declining merchants'
  }
}
