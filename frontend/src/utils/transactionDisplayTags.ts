import type { TransactionRow } from '../api/transaction'

export type TransactionDisplayTag = {
  id: string
  label: string
  color?: string
  hint?: string
}

/** Tags returned by backend finance semantics enrichment. */
export function transactionDisplayTags(row: TransactionRow): TransactionDisplayTag[] {
  return row.displayTags ?? []
}

/** Page-relative large amount hint (not from backend). */
export function isAnomalyAmount(amount: number, pageMaxAmount: number, ratio = 0.65, minAmount = 500): boolean {
  return pageMaxAmount > 0 && amount >= pageMaxAmount * ratio && amount >= minAmount
}
