import { describe, expect, it } from 'vitest'
import { aggregateTransactionRows, type TransactionRow } from './transaction'

describe('aggregateTransactionRows', () => {
  it('sums income and expense, skips transfers', () => {
    const rows: TransactionRow[] = [
      { id: '1', incomeMoney: 1000 },
      { id: '2', balanceMoney: -50 },
      { id: '3', balanceMoney: 200, txnKind: 'expense' },
      { id: '4', balanceMoney: 80, txnKind: 'transfer' },
    ]
    expect(aggregateTransactionRows(rows)).toEqual({
      income: 1050,
      expense: 200,
      net: 850,
      transfers: 1,
      unclassified: 3,
    })
  })

  it('counts classified rows only when category is set', () => {
    const rows: TransactionRow[] = [
      { id: '1', balanceMoney: -10, consumeName: 'Food' },
      { id: '2', balanceMoney: -20, consumeCode: 'x' },
      { id: '3', balanceMoney: -30 },
    ]
    expect(aggregateTransactionRows(rows).unclassified).toBe(1)
  })
})
