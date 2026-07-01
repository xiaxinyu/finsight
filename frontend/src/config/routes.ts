import { reportConfigs } from './reports'
import { reportNavGroupForId } from './reportNavigation'

export type RouteMeta = {
  path: string
  title: string
  breadcrumb: string[]
  group?: string
}

const staticRoutes: RouteMeta[] = [
  { path: '/dashboard', title: 'Financial Pulse', breadcrumb: ['Dashboard'] },
  { path: '/planning', title: 'Planning', breadcrumb: ['Planning'] },
  { path: '/wealth', title: 'Wealth', breadcrumb: ['Wealth'] },
  { path: '/goals', title: 'Goals', breadcrumb: ['Goals'] },
  { path: '/profile', title: 'Financial Profile', breadcrumb: ['Profile'] },
  { path: '/settings/account', title: 'Account & security', breadcrumb: ['Settings', 'Account'] },
  { path: '/transactions', title: 'Transactions', breadcrumb: ['Transactions', 'Detail'] },
  { path: '/statements/upload', title: 'Import Statement', breadcrumb: ['Transactions', 'Import'] },
  { path: '/statements', title: 'Import History', breadcrumb: ['Transactions', 'Import History'] },
  { path: '/admin/users', title: 'Users', breadcrumb: ['Admin', 'Users'] },
  { path: '/admin/cards', title: 'Bank Cards', breadcrumb: ['Admin', 'Bank Cards'] },
  { path: '/admin/rules', title: 'Rule Engine', breadcrumb: ['Admin', 'Rule Engine'] },
  { path: '/admin/categories', title: 'Categories', breadcrumb: ['Admin', 'Categories'] },
]

/** Legacy report slugs not wired in reportConfigs (deep links, tests). */
const legacyReportTitles: Record<string, string> = {
  'transaction-trend': 'Transaction Trend',
  'category-breakdown': 'Category Breakdown',
  'category-comparison': 'Category Comparison',
  'weekly-summary': 'Weekly Summary',
  'monthly-comparison': 'Monthly Comparison',
  'income-vs-expense': 'Income vs Expense',
  'expense-curve': 'Expense Curve',
  'income-curve': 'Income Curve',
}

const ledgerTitles: Record<string, string> = {
  salary: 'Income Ledger',
  expense: 'Expense Ledger',
  'house-rent': 'Rent Ledger',
  endowment: 'Pension Ledger',
  accumulation: 'Provident Fund',
  medical: 'Medical Insurance',
  unemployment: 'Unemployment Insurance',
}

export function resolveRouteMeta(pathname: string): RouteMeta {
  const exact = staticRoutes.find((r) => r.path === pathname)
  if (exact) return exact

  if (pathname.startsWith('/reports/')) {
    const id = pathname.replace('/reports/', '').split('/')[0]
    const cfg = reportConfigs[id]
    const title = cfg?.title ?? legacyReportTitles[id] ?? 'Report'
    const navGroup = reportNavGroupForId(id)
    const breadcrumb = navGroup ? ['Reports', navGroup.label, title] : ['Reports', title]
    return { path: pathname, title, breadcrumb, group: navGroup?.label ?? 'Reports' }
  }

  if (pathname.startsWith('/ledgers/')) {
    const id = pathname.replace('/ledgers/', '')
    const title = ledgerTitles[id] || 'Ledger'
    return { path: pathname, title, breadcrumb: ['Ledgers', title], group: 'Ledgers' }
  }

  return { path: pathname, title: 'FinSight', breadcrumb: ['FinSight'] }
}
