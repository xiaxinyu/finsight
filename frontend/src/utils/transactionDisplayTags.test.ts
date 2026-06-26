import { describe, expect, it } from 'vitest'
import { transactionDisplayTags } from './transactionDisplayTags'
import type { TransactionRow } from '../api/transaction'

describe('transactionDisplayTags', () => {
  it('returns backend tags when present', () => {
    const row: TransactionRow = {
      id: '1',
      displayTags: [{ id: 'fixed_cost', label: 'Fixed cost', color: 'purple' }],
    }
    expect(transactionDisplayTags(row)).toHaveLength(1)
    expect(transactionDisplayTags(row)[0].label).toBe('Fixed cost')
  })

  it('returns empty when backend omitted tags', () => {
    expect(transactionDisplayTags({ id: '1' })).toEqual([])
  })
})
