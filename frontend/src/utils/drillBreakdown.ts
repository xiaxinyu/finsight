import type { TransactionRow } from '../api/transaction'

export type DrillBreakdownItem = {
  code?: string
  token?: string
  label: string
  txnCount: number
  total: number
}

export type DrillBreakdownResult = {
  total: number
  sampleSize: number
  truncated: boolean
  aggregateTotal: number
  categories: DrillBreakdownItem[]
  merchants: DrillBreakdownItem[]
  transactions: TransactionRow[]
}

export function formatPartialDrillMessage(total: number, sampleSize: number): string {
  if (total <= sampleSize) {
    return ''
  }
  return `Showing ${sampleSize.toLocaleString()} of ${total.toLocaleString()} matching transactions. `
    + 'Breakdown totals are complete; the transaction list below is a sample.'
}

export function isDrillTruncated(result: Pick<DrillBreakdownResult, 'total' | 'sampleSize' | 'truncated'> | null | undefined): boolean {
  if (!result) return false
  return result.truncated || result.total > result.sampleSize
}

export function mapBreakdownRows(items: DrillBreakdownItem[] | undefined, kind: 'category' | 'merchant') {
  return (items || []).map((item) => ({
    key: kind === 'merchant' ? (item.token || item.label) : (item.code || item.label),
    label: item.label,
    kind,
    count: item.txnCount,
    total: item.total,
  }))
}
