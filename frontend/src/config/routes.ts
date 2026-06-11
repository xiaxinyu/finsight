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
  { path: '/transactions', title: 'Transactions', breadcrumb: ['Transactions', 'Detail'] },
  { path: '/statements/upload', title: 'Import Statement', breadcrumb: ['Transactions', 'Import'] },
  { path: '/statements', title: 'Import History', breadcrumb: ['Transactions', 'Import History'] },
  { path: '/admin/users', title: 'Users', breadcrumb: ['Admin', 'Users'] },
  { path: '/admin/cards', title: 'Bank Cards', breadcrumb: ['Admin', 'Bank Cards'] },
  { path: '/admin/rules', title: 'Rule Engine', breadcrumb: ['Admin', 'Rule Engine'] },
  { path: '/admin/categories', title: 'Categories', breadcrumb: ['Admin', 'Categories'] },
]

const reportTitles: Record<string, string> = {
  cashflow: 'Cashflow',
  'budget-vs-actual': 'Budget vs Actual',
  'fixed-vs-variable': 'Fixed vs Variable',
  'spending-drift': 'Spending Drift',
  'bills-calendar': 'Bills Calendar',
  'transaction-trend': 'Transaction Trend',
  'category-breakdown': 'Category Breakdown',
  'category-comparison': 'Category Comparison',
  'weekly-summary': 'Weekly Summary',
  'monthly-comparison': 'Monthly Comparison',
  'income-vs-expense': 'Income vs Expense',
  'income-curve': 'Income Curve',
  'expense-curve': 'Expense Curve',
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
    const id = pathname.replace('/reports/', '')
    const title = reportTitles[id] || 'Report'
    const group = id.includes('income') ? 'Income' : id.includes('expense') ? 'Expense' : 'Reports'
    return { path: pathname, title, breadcrumb: [group, title], group }
  }

  if (pathname.startsWith('/ledgers/')) {
    const id = pathname.replace('/ledgers/', '')
    const title = ledgerTitles[id] || 'Ledger'
    const group = id === 'salary' || id.includes('income') ? 'Income' : id === 'expense' || id === 'house-rent' ? 'Expense' : 'Benefits'
    return { path: pathname, title, breadcrumb: [group, title], group }
  }

  return { path: pathname, title: 'FinSight', breadcrumb: ['FinSight'] }
}
