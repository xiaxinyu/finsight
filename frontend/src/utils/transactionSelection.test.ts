import { describe, expect, it } from 'vitest'
import type { TransactionRow } from '../api/transaction'
import { summarizeSelection } from './transactionSelection'

describe('transactionSelection', () => {
  it('summarizes income, expense, and date span', () => {
    const rows: TransactionRow[] = [
      { id: '1', transactionDate: '2026-03-10', incomeMoney: 5000, balanceMoney: 0, consumeCode: 'SAL' },
      { id: '2', transactionDate: '2026-03-15', balanceMoney: 120, consumeCode: 'FOOD' },
      { id: '3', transactionDate: '2026-03-20', balanceMoney: 80, consumeCode: 'FOOD' },
    ]
    const summary = summarizeSelection(rows)
    expect(summary.count).toBe(3)
    expect(summary.income).toBe(5000)
    expect(summary.expense).toBe(200)
    expect(summary.net).toBe(4800)
    expect(summary.dateFrom).toBeTruthy()
    expect(summary.dateTo).toBeTruthy()
  })
})
