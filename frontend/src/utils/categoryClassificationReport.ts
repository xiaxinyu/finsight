import type { ReportPoint } from '../api/report'

export type CategoryClassificationRow = {
  key: string
  classification: string
  txnType: 'Expense' | 'Income'
  amount: number
  sharePct: number
  level1Code?: string
  level1Name?: string
  categoryCode?: string
  categoryName?: string
}

/** L1 / L2 path for Reporting Classification tree display. */
export function formatCategoryClassificationPath(
  level1Name?: string,
  categoryName?: string,
  level1Code?: string,
  categoryCode?: string,
): string {
  const l1 = (level1Name || level1Code || '').trim()
  const l2 = (categoryName || categoryCode || '').trim()
  if (!l1 && !l2) return '未分类'
  if (!l1 || !l2 || l1 === l2 || level1Code === categoryCode) return l1 || l2
  return `${l1} / ${l2}`
}

export function buildCategoryClassificationRows(
  points: ReportPoint[],
  txnType: 'expense' | 'income' = 'expense',
  totalOverride?: number,
): CategoryClassificationRow[] {
  const txnLabel: CategoryClassificationRow['txnType'] = txnType === 'income' ? 'Income' : 'Expense'
  const filtered = points.filter((p) => Number.isFinite(p.value) && p.value > 0)
  const total = totalOverride ?? filtered.reduce((s, p) => s + p.value, 0)
  return filtered
    .sort((a, b) => b.value - a.value)
    .map((p) => {
      const categoryName = p.name || p.key
      const classification = formatCategoryClassificationPath(
        p.level1Name,
        categoryName,
        p.level1Code,
        p.code,
      )
      return {
        key: p.code || p.key,
        classification,
        txnType: txnLabel,
        amount: p.value,
        sharePct: total > 0 ? (p.value / total) * 100 : 0,
        level1Code: p.level1Code,
        level1Name: p.level1Name,
        categoryCode: p.code,
        categoryName,
      }
    })
}
