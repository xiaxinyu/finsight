export type TransactionSortField = 'transactionDate' | 'amount' | 'card' | 'type'
export type TransactionSortOrder = 'asc' | 'desc'

const COLUMN_TO_FIELD: Record<string, TransactionSortField> = {
  transactionDate: 'transactionDate',
  editAmount: 'amount',
  bankCode: 'card',
  txnKind: 'type',
}

/** Map ProTable sorter state to whitelisted backend sort params. */
export function mapTransactionTableSort(
  sort?: Record<string, 'ascend' | 'descend' | null | undefined>,
): { sortField?: TransactionSortField; sortOrder?: TransactionSortOrder } {
  if (!sort) return {}
  for (const [column, direction] of Object.entries(sort)) {
    const field = COLUMN_TO_FIELD[column]
    if (!field || !direction) continue
    return {
      sortField: field,
      sortOrder: direction === 'ascend' ? 'asc' : 'desc',
    }
  }
  return {}
}
