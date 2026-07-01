import { describe, expect, it } from 'vitest'
import {
  REPORT_NAV_GROUPS,
  menuOpenKeysForReportId,
  reportNavGroupForId,
  reportNavGroupKey,
} from './reportNavigation'

describe('reportNavigation', () => {
  it('maps every configured report to exactly one nav group', () => {
    const ids = REPORT_NAV_GROUPS.flatMap((g) => [...g.reportIds])
    expect(new Set(ids).size).toBe(ids.length)
    expect(reportNavGroupKey('income-trends')).toBe('reports-yoy')
    expect(reportNavGroupKey('fixed-vs-variable')).toBe('reports-spending')
    expect(reportNavGroupKey('transfer-finance')).toBe('reports-capital')
  })

  it('returns group metadata for breadcrumbs', () => {
    expect(reportNavGroupForId('cashflow')?.label).toBe('Monthly overview')
    expect(reportNavGroupForId('debt-trends')?.label).toBe('Year-over-year trends')
  })

  it('builds menu open keys from report id', () => {
    expect(menuOpenKeysForReportId('bills-calendar')).toEqual(['reports', 'reports-monthly'])
    expect(menuOpenKeysForReportId('merchant-drift')).toEqual(['reports', 'reports-merchants'])
  })
})
