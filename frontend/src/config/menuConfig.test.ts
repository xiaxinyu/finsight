import { describe, expect, it } from 'vitest'
import { menuOpenKeysForPath, reconcileMenuOpenKeys } from './menuConfig'

describe('menu navigation helpers', () => {
  it('opens report section keys for spending reports', () => {
    expect(menuOpenKeysForPath('/reports/fixed-vs-variable')).toEqual(['reports', 'reports-spending'])
  })

  it('opens ledger benefits nested keys', () => {
    expect(menuOpenKeysForPath('/ledgers/medical')).toEqual(['ledgers', 'ledgers-benefits'])
  })

  it('accordion keeps only one root section', () => {
    const prev = ['transactions']
    const next = ['transactions', 'reports', 'reports-cashflow']
    expect(reconcileMenuOpenKeys(prev, next)).toEqual(['reports', 'reports-cashflow'])
  })

  it('accordion preserves nested keys under active root', () => {
    const prev = ['reports', 'reports-spending']
    const next = ['reports', 'reports-spending', 'reports-cashflow']
    expect(reconcileMenuOpenKeys(prev, next)).toEqual(['reports', 'reports-spending', 'reports-cashflow'])
  })
})
