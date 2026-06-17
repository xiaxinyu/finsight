import type { EChartsOption } from 'echarts'
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
  drillDown?: Record<string, string>
}

export type SubscriptionReport = {
  subscriptions: MerchantSubscription[]
  summary: {
    count: number
    monthlyTotal: number
    annualizedTotal: number
    optimizableAmount: number
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
    { key: 'count', label: 'Subscriptions', value: String(s.count) },
    { key: 'monthly', label: 'Monthly total', value: formatMoney(s.monthlyTotal), tone: 'expense' as const },
    { key: 'annual', label: 'Annualized', value: formatMoney(s.annualizedTotal), tone: 'expense' as const },
    {
      key: 'opt',
      label: 'Optimizable / yr',
      value: formatMoney(s.optimizableAmount),
      tone: s.optimizableAmount > 0 ? 'warn' as const : 'neutral' as const,
    },
  ]
}

export function buildConcentrationKpis(report: MerchantConcentrationReport) {
  return [
    { key: 'total', label: 'Total spend', value: formatMoney(report.totalSpend), tone: 'expense' as const },
    { key: 'merchants', label: 'Merchants', value: String(report.merchantCount) },
    { key: 'top1', label: 'Top 1 share', value: `${(report.top1SharePct ?? 0).toFixed(1)}%` },
    { key: 'top5', label: 'Top 5 share', value: `${(report.top5SharePct ?? report.top3SharePct).toFixed(1)}%` },
  ]
}

export function buildConcentrationChart(report: MerchantConcentrationReport): EChartsOption {
  const top = report.merchants.slice(0, 10)
  return {
    grid: { left: 48, right: 16, top: 48, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: top.map((m) => m.displayName), axisLabel: { fontSize: 10, rotate: 25 } },
    yAxis: { type: 'value', axisLabel: { formatter: (v: number) => `${v}%` } },
    series: [{
      name: 'Share',
      type: 'bar',
      data: top.map((m) => m.sharePct),
      itemStyle: { color: '#2563eb' },
      barMaxWidth: 28,
    }],
  }
}

export function buildDriftChart(report: MerchantDriftReport): EChartsOption {
  const movers = report.movers.slice(0, 12)
  return {
    grid: { left: 48, right: 16, top: 48, bottom: 28 },
    tooltip: { trigger: 'axis' },
    legend: { data: [String(report.priorYear), String(report.year)], top: 4 },
    xAxis: { type: 'category', data: movers.map((m) => m.displayName), axisLabel: { fontSize: 10, rotate: 25 } },
    yAxis: { type: 'value' },
    series: [
      { name: String(report.priorYear), type: 'bar', data: movers.map((m) => m.priorSpend), itemStyle: { color: '#94a3b8' } },
      { name: String(report.year), type: 'bar', data: movers.map((m) => m.currentSpend), itemStyle: { color: '#2563eb' } },
    ],
  }
}

export function buildSubscriptionInsights(report: SubscriptionReport) {
  const s = report.summary
  const bullets = [
    {
      text: s.count
        ? `${s.count} suspected subscription(s) totaling ${formatMoney(s.monthlyTotal)}/month.`
        : 'No recurring subscriptions detected — refresh merchant profiles after importing transactions.',
      warn: s.count === 0,
    },
  ]
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
  const cv = sub.amountCv != null ? `${Math.round(sub.amountCv * 100)}% amount CV` : ''
  const interval = sub.avgIntervalDays ? `~${Math.round(sub.avgIntervalDays)}d interval` : ''
  return [interval, cv].filter(Boolean).join(' · ') || '—'
}

export function driftBucketLabel(bucket: 'new' | 'growing' | 'declining'): string {
  switch (bucket) {
    case 'new': return 'New merchants'
    case 'growing': return 'Growing merchants'
    case 'declining': return 'Declining merchants'
  }
}
