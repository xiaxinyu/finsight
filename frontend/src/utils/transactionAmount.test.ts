import { describe, expect, it } from 'vitest'
import { rowAmount, rowTxnKind } from './transactionAmount'

describe('rowAmount', () => {
  it('prefers incomeMoney when set', () => {
    expect(rowAmount({ incomeMoney: 100, balanceMoney: 50 })).toBe(100)
  })

  it('uses balanceMoney magnitude when income is zero', () => {
    expect(rowAmount({ incomeMoney: 0, balanceMoney: 88 })).toBe(88)
  })

  it('handles legacy negative balance as income magnitude via rowTxnKind', () => {
    expect(rowTxnKind({ balanceMoney: -42 })).toBe('income')
  })
})
