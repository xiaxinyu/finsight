import type { ReportPoint } from '../api/report'

export type SemanticBreakdownRow = {
  tagId: string
  label: string
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
  rows: Array<Pick<SemanticBreakdownRow, 'tagId' | 'label' | 'amount'>>,
): ReportPoint[] {
  return rows.map((r) => ({
    key: r.tagId,
    value: r.amount,
    code: r.tagId,
    name: r.label,
  }))
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
    bullets.push({ text: `Largest bucket: ${top.label} (${top.sharePct.toFixed(1)}%, ${formatCompact(top.amount)}).` })
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
