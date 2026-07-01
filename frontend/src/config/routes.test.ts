import { describe, expect, it } from 'vitest'
import { resolveRouteMeta } from './routes'

describe('resolveRouteMeta', () => {
  it('resolves static dashboard route', () => {
    const meta = resolveRouteMeta('/dashboard')
    expect(meta.title).toBe('Financial Pulse')
    expect(meta.breadcrumb).toEqual(['Dashboard'])
  })

  it('resolves canonical report routes with nav group breadcrumbs', () => {
    expect(resolveRouteMeta('/reports/cashflow').title).toBe('Cashflow')
    expect(resolveRouteMeta('/reports/budget-vs-actual').title).toBe('Budget vs Actual')
    expect(resolveRouteMeta('/reports/spending-drift').title).toBe('Period Comparison')
    expect(resolveRouteMeta('/reports/cashflow').breadcrumb).toEqual([
      'Reports',
      'Monthly overview',
      'Cashflow',
    ])
    expect(resolveRouteMeta('/reports/budget-vs-actual').breadcrumb).toEqual([
      'Reports',
      'Monthly overview',
      'Budget vs Actual',
    ])
    expect(resolveRouteMeta('/reports/spending-drift').breadcrumb).toEqual([
      'Reports',
      'Spending analysis',
      'Period Comparison',
    ])
  })

  it('resolves year-over-year trend reports', () => {
    const meta = resolveRouteMeta('/reports/trend-changes')
    expect(meta.title).toBe('Consumption Trends')
    expect(meta.breadcrumb).toEqual(['Reports', 'Year-over-year trends', 'Consumption Trends'])
  })

  it('resolves legacy report without nav group', () => {
    const meta = resolveRouteMeta('/reports/income-vs-expense')
    expect(meta.title).toBe('Income vs Expense')
    expect(meta.breadcrumb).toEqual(['Reports', 'Income vs Expense'])
  })

  it('resolves generic legacy report under Reports group', () => {
    const meta = resolveRouteMeta('/reports/category-breakdown')
    expect(meta.breadcrumb).toEqual(['Reports', 'Category Breakdown'])
  })

  it('resolves ledger route', () => {
    const meta = resolveRouteMeta('/ledgers/salary')
    expect(meta.title).toBe('Income Ledger')
    expect(meta.breadcrumb).toEqual(['Ledgers', 'Income Ledger'])
  })

  it('resolves admin nested route', () => {
    const meta = resolveRouteMeta('/admin/users')
    expect(meta.breadcrumb).toEqual(['Admin', 'Users'])
  })
})
