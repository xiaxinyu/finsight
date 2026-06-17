import { describe, expect, it } from 'vitest'
import { mapTransactionTableSort } from './transactionTableSort'

describe('mapTransactionTableSort', () => {
  it('maps ProTable column keys to whitelisted backend fields', () => {
    expect(mapTransactionTableSort({ transactionDate: 'ascend' })).toEqual({
      sortField: 'transactionDate',
      sortOrder: 'asc',
    })
    expect(mapTransactionTableSort({ editAmount: 'descend' })).toEqual({
      sortField: 'amount',
      sortOrder: 'desc',
    })
    expect(mapTransactionTableSort({ bankCode: 'ascend' })).toEqual({
      sortField: 'card',
      sortOrder: 'asc',
    })
    expect(mapTransactionTableSort({ txnKind: 'descend' })).toEqual({
      sortField: 'type',
      sortOrder: 'desc',
    })
  })

  it('ignores unknown columns and empty sorter state', () => {
    expect(mapTransactionTableSort({ transactionDesc: 'ascend' })).toEqual({})
    expect(mapTransactionTableSort({ transactionDate: null })).toEqual({})
    expect(mapTransactionTableSort(undefined)).toEqual({})
    expect(mapTransactionTableSort({})).toEqual({})
  })

  it('uses the first active sorter entry', () => {
    expect(mapTransactionTableSort({
      transactionDate: 'ascend',
      editAmount: 'descend',
    })).toEqual({
      sortField: 'transactionDate',
      sortOrder: 'asc',
    })
  })
})
