import { describe, expect, it } from 'vitest'
import type { TransactionRow } from '../api/transaction'
import { amountIntensity, detectTransactionRiskTags, isUnclassifiedRow } from './transactionRisk'

const base: TransactionRow = {
  id: '1',
  transactionDesc: 'Coffee shop',
  balanceMoney: 35,
  consumeCode: 'FOOD',
  consumeName: 'Food',
}

describe('transactionRisk', () => {
  it('flags unclassified rows', () => {
    expect(isUnclassifiedRow({ ...base, consumeCode: '', consumeName: '' })).toBe(true)
    expect(detectTransactionRiskTags({ ...base, consumeCode: '', consumeName: '' })).toContain('unclassified')
  })

  it('detects subscription and fixed-cost hints', () => {
    expect(detectTransactionRiskTags({
      ...base,
      transactionDesc: 'Netflix monthly',
      consumeCode: 'SUB',
      consumeName: 'Subscriptions',
    })).toContain('subscription')

    expect(detectTransactionRiskTags({
      ...base,
      transactionDesc: 'Rent payment',
      consumeCode: 'HOME',
      consumeName: 'Housing',
    })).toContain('fixed_cost')
  })

  it('flags transfer and refund candidates', () => {
    expect(detectTransactionRiskTags({ ...base, transactionDesc: 'ATM transfer' }))
      .toContain('transfer_candidate')
    expect(detectTransactionRiskTags({ ...base, transactionDesc: 'Refund from store' }))
      .toContain('refund_candidate')
  })

  it('marks anomaly relative to page max', () => {
    const tags = detectTransactionRiskTags(
      { ...base, balanceMoney: 9000 },
      { amountMax: 10000, anomalyRatio: 0.65 },
    )
    expect(tags).toContain('anomaly')
    expect(amountIntensity(9000, 10000)).toBe(90)
  })
})
