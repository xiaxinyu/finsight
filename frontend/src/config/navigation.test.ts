import { describe, expect, it } from 'vitest'
import { ledgerIds } from './ledgers'
import { reportIds } from './reports'
import { menuItems, type FsMenuItem } from './menuConfig'

function collectPaths(items: FsMenuItem[]): string[] {
  return items.flatMap((item) => {
    const paths = item.path ? [item.path] : []
    const childItems = item.children ?? []
    return paths.concat(childItems.length ? collectPaths(childItems) : [])
  })
}

describe('navigation config', () => {
  it('menu report paths have matching report configs', () => {
    const reportPaths = collectPaths(menuItems)
      .filter((p) => p.startsWith('/reports/'))
      .map((p) => p.replace('/reports/', ''))
    reportPaths.forEach((id) => expect(reportIds).toContain(id))
  })

  it('menu ledger paths have matching ledger configs', () => {
    const ledgerPaths = collectPaths(menuItems)
      .filter((p) => p.startsWith('/ledgers/'))
      .map((p) => p.replace('/ledgers/', ''))
      .filter((id) => id !== 'loans')
    ledgerPaths.forEach((id) => expect(ledgerIds).toContain(id))
  })
})
