import { describe, expect, it } from 'vitest'
import { menuOpenKeysForPath, reconcileMenuOpenKeys } from './menuConfig'

describe('menu navigation helpers', () => {
  it('opens report section keys for spending analysis reports', () => {
    expect(menuOpenKeysForPath('/reports/fixed-vs-variable')).toEqual(['reports', 'reports-spending'])
  })

  it('opens year-over-year group for trend reports', () => {
    expect(menuOpenKeysForPath('/reports/income-trends')).toEqual(['reports', 'reports-yoy'])
    expect(menuOpenKeysForPath('/reports/trend-changes')).toEqual(['reports', 'reports-yoy'])
    expect(menuOpenKeysForPath('/reports/debt-trends')).toEqual(['reports', 'reports-yoy'])
  })

  it('opens ledger benefits nested keys', () => {
    expect(menuOpenKeysForPath('/ledgers/medical')).toEqual(['ledgers', 'ledgers-benefits'])
  })

  it('accordion keeps only one root section', () => {
    const prev = ['transactions']
    const next = ['transactions', 'reports', 'reports-monthly']
    expect(reconcileMenuOpenKeys(prev, next)).toEqual(['reports', 'reports-monthly'])
  })

  it('accordion preserves nested keys under active root', () => {
    const prev = ['reports', 'reports-spending']
    const next = ['reports', 'reports-spending', 'reports-yoy']
    expect(reconcileMenuOpenKeys(prev, next)).toEqual(['reports', 'reports-spending', 'reports-yoy'])
  })
})
