import { describe, expect, it } from 'vitest'
import { drillBreadcrumbs, previousLayer } from './layerNav'

describe('layerNav', () => {
  it('builds breadcrumbs for insight layer', () => {
    expect(drillBreadcrumbs('insight', null).map((c) => c.layer)).toEqual(['insight', 'breakdown'])
  })

  it('includes merchant and actions crumbs when applicable', () => {
    const crumbs = drillBreadcrumbs('actions', 'Amazon')
    expect(crumbs.map((c) => c.title)).toEqual(['Insight', 'Breakdown', 'Amazon', 'Actions'])
  })

  it('steps back through layers', () => {
    expect(previousLayer('breakdown', null)).toBe('insight')
    expect(previousLayer('transactions', 'Amazon')).toBe('breakdown')
    expect(previousLayer('actions', null)).toBe('breakdown')
    expect(previousLayer('actions', 'Amazon')).toBe('transactions')
    expect(previousLayer('insight', null)).toBeNull()
  })
})
