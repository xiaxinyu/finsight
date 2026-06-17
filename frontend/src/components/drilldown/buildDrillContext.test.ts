import { describe, expect, it } from 'vitest'
import {
  buildDashboardDrillContext,
  buildReportDrillContext,
  drillParamsForCategory,
  drillParamsForMerchant,
  drillParamsForMonth,
  drillParamsForYearMonth,
  mergeDrillActions,
} from './buildDrillContext'

describe('buildDrillContext', () => {
  it('buildReportDrillContext uses insights when provided', () => {
    const ctx = buildReportDrillContext({
      title: 'Food · Jan',
      metricLabel: 'Food',
      params: { transactionDateStartStr: '2026-01-01', transactionDateEndStr: '2026-01-31', consumeName: 'Food' },
      explanation: ['Food spend rose 12%'],
    })
    expect(ctx.explanation).toEqual(['Food spend rose 12%'])
    expect(ctx.source).toBe('report')
  })

  it('mergeDrillActions deduplicates by path', () => {
    const actions = mergeDrillActions([
      { label: 'Planning', type: 'planning', path: '/planning' },
    ])
    expect(actions.filter((a) => a.path === '/planning')).toHaveLength(1)
    expect(actions.length).toBeGreaterThan(1)
  })

  it('drillParamsForMonth maps month label to date range', () => {
    expect(drillParamsForMonth('Mar', 2026)).toEqual({
      transactionDateStartStr: '2026-03-01',
      transactionDateEndStr: '2026-03-31',
      txnTypes: 'expense',
    })
  })

  it('drillParamsForYearMonth parses YYYY-MM', () => {
    expect(drillParamsForYearMonth('2026-05')).toMatchObject({
      transactionDateStartStr: '2026-05-01',
      transactionDateEndStr: '2026-05-31',
    })
  })

  it('drillParamsForMerchant includes stable token', () => {
    expect(drillParamsForMerchant('netflix', 'Netflix', '2025-01-01', '2026-12-31')).toEqual({
      transactionDateStartStr: '2025-01-01',
      transactionDateEndStr: '2026-12-31',
      txnTypes: 'expense',
      merchantToken: 'netflix',
      merchantLabel: 'Netflix',
    })
  })

  it('buildDashboardDrillContext tags source dashboard', () => {
    const ctx = buildDashboardDrillContext({
      title: 'Dash',
      metricLabel: 'Net',
      params: drillParamsForCategory('Food', '2026-01-01', '2026-01-31'),
      explanation: ['line'],
    })
    expect(ctx.source).toBe('dashboard')
  })
})
