import { describe, expect, it } from 'vitest'
import { aggregateTransactionRows, parseReclassifyResult, type TransactionRow } from './transaction'

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

  it('parses reclassify JSON string from postCommon', () => {
    const payload = {
      requested: 2,
      classified: 1,
      skipped: 0,
      noMatch: 1,
      dryRun: true,
      preview: [{ id: 'a', categoryCode: 'FOOD', categoryName: 'Food', action: 'PREVIEW' }],
    }
    const parsed = parseReclassifyResult(JSON.stringify(payload))
    expect(parsed).toEqual(payload)
  })

  it('parses reclassify object nested in CommonResult.data', () => {
    const inner = { requested: 1, classified: 1, skipped: 0, noMatch: 0, dryRun: true, preview: [] }
    const parsed = parseReclassifyResult({ data: JSON.stringify(inner) })
    expect(parsed).toEqual(inner)
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
