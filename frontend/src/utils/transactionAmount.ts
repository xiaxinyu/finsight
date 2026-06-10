/** Canonical row amount: incomeMoney first, else positive balanceMoney magnitude. */
export function rowAmount(row: { incomeMoney?: number | null; balanceMoney?: number | null }): number {
  const income = Math.abs(Number(row.incomeMoney || 0))
  if (income > 0) return income
  return Math.abs(Number(row.balanceMoney || 0))
}

export function rowTxnKind(row: {
  incomeMoney?: number | null
  balanceMoney?: number | null
  txnKind?: string | null
}): 'income' | 'expense' {
  if (row.txnKind === 'income' || row.txnKind === 'expense') return row.txnKind
  if (Number(row.incomeMoney) > 0) return 'income'
  if (row.balanceMoney != null && row.balanceMoney < 0) return 'income'
  return 'expense'
}
