import type { ReportPoint } from '../api/report'
import { formatMoney } from './format'

const PIE_COLORS = [
  '#2563eb', '#16a34a', '#ea580c', '#7c3aed', '#0891b2',
  '#db2777', '#ca8a04', '#64748b', '#dc2626', '#0d9488',
]

export type ChartSummaryItem = {
  key: string
  label: string
  value: string
  tone?: 'expense' | 'income' | 'neutral' | 'warn'
}

export type SemanticBreakdownRow = {
  tagId: string
  label: string
  classL1: string
  classL2: string
  classification: string
  txnType: 'Expense' | 'Income'
  group: 'expense' | 'fixed' | 'income' | 'capital' | 'other'
  amount: number
  sharePct: number
}

export type SemanticBreakdown = {
  rows: SemanticBreakdownRow[]
  expenseTotal: number
  fixedTotal: number
  variableTotal: number
  fixedSharePct: number
  variableSharePct: number
  metricsSource?: string
  periodStart?: string
  periodEnd?: string
}

export function isDrillableSemanticTag(tagId?: string | null): boolean {
  if (!tagId || tagId === 'Total') return false
  return tagId !== 'other_combined'
}

export function semanticBreakdownToReportPoints(
  rows: Array<Pick<SemanticBreakdownRow, 'tagId' | 'label' | 'classification' | 'amount'>>,
): ReportPoint[] {
  return rows.map((r) => ({
    key: r.tagId,
    value: r.amount,
    code: r.tagId,
    name: r.classification || r.label,
  }))
}

export function buildSemanticClassificationChartOption(
  breakdown: SemanticBreakdown,
  topRows: SemanticBreakdownRow[],
): { option: Record<string, unknown>; summary: ChartSummaryItem[] } {
  const sorted = [...breakdown.rows].sort((a, b) => b.amount - a.amount)
  const top = sorted[0]
  const top3Share = sorted.slice(0, 3).reduce((s, r) => s + r.sharePct, 0)

  const data = topRows.map((r) => ({
    name: r.classification || r.label,
    value: r.amount,
    tagId: r.tagId,
    sharePct: r.sharePct,
    classL2: r.classL2 || r.label,
  }))
  const metaByName = new Map(data.map((d) => [d.name, d]))

  const summary: ChartSummaryItem[] = [
    { key: 'total', label: 'Total expense', value: formatMoney(breakdown.expenseTotal), tone: 'expense' },
    { key: 'items', label: 'Classifications', value: String(breakdown.rows.length) },
    ...(top ? [{ key: 'top', label: 'Largest', value: `${top.classL2} · ${top.sharePct.toFixed(1)}%` }] : []),
    { key: 'top3', label: 'Top 3 share', value: `${top3Share.toFixed(1)}%` },
    { key: 'fixed', label: 'Fixed', value: `${breakdown.fixedSharePct.toFixed(1)}%` },
    { key: 'var', label: 'Variable', value: `${breakdown.variableSharePct.toFixed(1)}%` },
  ]

  return {
    summary,
    option: {
      color: PIE_COLORS,
      tooltip: {
        trigger: 'item',
        backgroundColor: '#ffffff',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        padding: [10, 14],
        extraCssText: 'box-shadow: 0 4px 16px rgba(15,23,42,0.08); border-radius: 8px;',
        confine: true,
        formatter: (p: { name?: string; value?: number; percent?: number; data?: { sharePct?: number } }) => {
          const share = p.data?.sharePct ?? p.percent ?? 0
          return `<div style="font-size:12px;line-height:1.55"><b>${p.name ?? ''}</b><br/>${formatMoney(Number(p.value ?? 0))}<br/><span style="color:#64748b">${Number(share).toFixed(1)}% of total</span></div>`
        },
      },
      legend: {
        type: 'scroll',
        orient: 'horizontal',
        bottom: 4,
        left: 'center',
        width: '94%',
        itemGap: 14,
        itemWidth: 8,
        itemHeight: 8,
        pageIconSize: 10,
        textStyle: { fontSize: 11, color: '#475569' },
        formatter: (name: string) => {
          const item = metaByName.get(name)
          if (!item) return name
          return `${item.classL2}  ${item.sharePct.toFixed(1)}%`
        },
      },
      graphic: breakdown.expenseTotal > 0 ? [{
        type: 'group',
        left: 'center',
        top: '36%',
        children: [
          {
            type: 'text',
            left: 'center',
            top: 0,
            style: {
              text: formatMoney(breakdown.expenseTotal),
              textAlign: 'center',
              fill: '#0f172a',
              fontSize: 18,
              fontWeight: 600,
              fontFamily: 'inherit',
            },
          },
          {
            type: 'text',
            left: 'center',
            top: 24,
            style: {
              text: 'Total expense',
              textAlign: 'center',
              fill: '#64748b',
              fontSize: 11,
              fontFamily: 'inherit',
            },
          },
          {
            type: 'text',
            left: 'center',
            top: 40,
            style: {
              text: `${breakdown.rows.length} classifications`,
              textAlign: 'center',
              fill: '#94a3b8',
              fontSize: 10,
              fontFamily: 'inherit',
            },
          },
        ],
      }] : [],
      series: [{
        type: 'pie',
        radius: ['44%', '66%'],
        center: ['50%', '40%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2,
        },
        emphasis: {
          scale: true,
          scaleSize: 5,
          itemStyle: { shadowBlur: 10, shadowColor: 'rgba(15,23,42,0.12)' },
        },
        data,
        label: { show: false },
        labelLine: { show: false },
      }],
    },
  }
}

export function topSemanticRows(rows: SemanticBreakdownRow[], topN = 10): SemanticBreakdownRow[] {
  const sorted = [...rows].sort((a, b) => b.amount - a.amount)
  if (sorted.length <= topN) return sorted
  const head = sorted.slice(0, topN)
  const rest = sorted.slice(topN)
  const otherAmount = rest.reduce((s, r) => s + r.amount, 0)
  const otherShare = rest.reduce((s, r) => s + r.sharePct, 0)
  if (otherAmount <= 0) return head
  return [...head, {
    tagId: 'other_combined',
    label: 'Other',
    classL1: 'Expense',
    classL2: 'Other',
    classification: 'Expense / Other',
    txnType: 'Expense' as const,
    group: 'expense',
    amount: otherAmount,
    sharePct: otherShare,
  }]
}

export function insightsSemanticStructure(
  breakdown: SemanticBreakdown,
  periodLabel: string,
): { text: string; warn?: boolean }[] {
  const bullets: { text: string; warn?: boolean }[] = []
  if (breakdown.expenseTotal <= 0) {
    return [{ text: 'No classified expense in this period.', warn: true }]
  }
  bullets.push({
    text: `${periodLabel}: fixed ${breakdown.fixedSharePct.toFixed(0)}% · variable ${breakdown.variableSharePct.toFixed(0)}% of expense trend.`,
  })
  if (breakdown.fixedSharePct >= 45) {
    bullets.push({
      text: 'Fixed burden exceeds 45% — review housing, utilities, and subscriptions.',
      warn: true,
    })
  }
  const top = breakdown.rows[0]
  if (top) {
    bullets.push({ text: `Largest bucket: ${top.classification || top.label} (${top.sharePct.toFixed(1)}%, ${formatCompact(top.amount)}).` })
  }
  const medical = breakdown.rows.find((r) => r.tagId === 'medical_spending')
  if (medical && medical.sharePct >= 8) {
    bullets.push({ text: `Medical spend is ${medical.sharePct.toFixed(1)}% of expenses — worth tracking separately.` })
  }
  return bullets
}

function formatCompact(amount: number): string {
  if (amount >= 10000) return `${(amount / 10000).toFixed(1)}万`
  if (amount >= 1000) return `${(amount / 1000).toFixed(1)}k`
  return String(Math.round(amount))
}
