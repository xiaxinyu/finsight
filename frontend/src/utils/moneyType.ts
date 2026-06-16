export function moneyTypeFromRow(txnType?: string, amount?: number | null): 'income' | 'expense' | 'neutral' {
  if (txnType === 'income') return 'income'
  if (txnType === 'expense') return 'expense'
  const n = Number(amount)
  if (n > 0) return 'income'
  if (n < 0) return 'expense'
  return 'neutral'
}

