import { describe, expect, it } from 'vitest'
import { resolveRouteMeta } from './routes'

describe('resolveRouteMeta', () => {
  it('resolves static dashboard route', () => {
    const meta = resolveRouteMeta('/dashboard')
    expect(meta.title).toBe('Financial Pulse')
    expect(meta.breadcrumb).toEqual(['Dashboard'])
  })

  it('resolves canonical report routes used by profile actions', () => {
    expect(resolveRouteMeta('/reports/cashflow').title).toBe('Cashflow')
    expect(resolveRouteMeta('/reports/budget-vs-actual').title).toBe('Budget vs Actual')
    expect(resolveRouteMeta('/reports/spending-drift').title).toBe('Spending Drift')
    expect(resolveRouteMeta('/reports/cashflow').breadcrumb).toEqual(['Reports', 'Cashflow'])
    expect(resolveRouteMeta('/reports/budget-vs-actual').breadcrumb).toEqual(['Reports', 'Budget vs Actual'])
    expect(resolveRouteMeta('/reports/spending-drift').breadcrumb).toEqual(['Reports', 'Spending Drift'])
  })

  it('resolves report route with group', () => {
    const meta = resolveRouteMeta('/reports/income-vs-expense')
    expect(meta.title).toBe('Income vs Expense')
    expect(meta.breadcrumb).toEqual(['Income', 'Income vs Expense'])
  })

  it('resolves generic report under Reports group', () => {
    const meta = resolveRouteMeta('/reports/category-breakdown')
    expect(meta.breadcrumb).toEqual(['Reports', 'Category Breakdown'])
  })

  it('resolves ledger route', () => {
    const meta = resolveRouteMeta('/ledgers/salary')
    expect(meta.title).toBe('Income Ledger')
    expect(meta.breadcrumb).toEqual(['Income', 'Income Ledger'])
  })

  it('resolves admin nested route', () => {
    const meta = resolveRouteMeta('/admin/users')
    expect(meta.breadcrumb).toEqual(['Admin', 'Users'])
  })
})
