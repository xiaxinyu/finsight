/**
 * Single source of truth for Reports sidebar grouping, labels, and route breadcrumbs.
 * Page titles live in `reports.ts`; menu labels here may be shorter.
 */

export type ReportNavGroupKey =
  | 'reports-monthly'
  | 'reports-yoy'
  | 'reports-spending'
  | 'reports-capital'
  | 'reports-forecast'
  | 'reports-merchants'

export type ReportNavGroup = {
  key: ReportNavGroupKey
  /** Sidebar submenu title */
  label: string
  /** One-line hint for docs / tooltips */
  description: string
  reportIds: readonly string[]
}

/** Ordered report groups under Reports → … */
export const REPORT_NAV_GROUPS: readonly ReportNavGroup[] = [
  {
    key: 'reports-monthly',
    label: 'Monthly overview',
    description: 'Earn, spend, and upcoming bills',
    reportIds: ['cashflow', 'budget-vs-actual', 'bills-calendar'],
  },
  {
    key: 'reports-yoy',
    label: 'Year-over-year trends',
    description: 'Compare income, spending, and debt by calendar year',
    reportIds: ['income-trends', 'trend-changes', 'debt-trends'],
  },
  {
    key: 'reports-spending',
    label: 'Spending analysis',
    description: 'Structure and custom period comparisons',
    reportIds: ['fixed-vs-variable', 'spending-drift'],
  },
  {
    key: 'reports-capital',
    label: 'Capital & taxes',
    description: 'Transfers, investments, loans, and tax — excluded from daily spending',
    reportIds: ['fund-flow', 'transfer-finance', 'tax-summary'],
  },
  {
    key: 'reports-forecast',
    label: 'Forecast & risk',
    description: 'Forward-looking cash and scenario planning',
    reportIds: ['annual-outlook', 'cash-risk'],
  },
  {
    key: 'reports-merchants',
    label: 'Merchants',
    description: 'Subscriptions and where money goes by merchant',
    reportIds: ['subscriptions', 'merchant-concentration', 'merchant-drift'],
  },
]

/** Shorter or clearer sidebar labels (fallback: title from reports.ts). */
export const REPORT_MENU_LABELS: Record<string, string> = {
  cashflow: 'Cashflow',
  'budget-vs-actual': 'Budget vs actual',
  'bills-calendar': 'Bills calendar',
  'income-trends': 'Income',
  'trend-changes': 'Consumption',
  'debt-trends': 'Debt',
  'fixed-vs-variable': 'Fixed vs variable',
  'spending-drift': 'Period comparison',
  'fund-flow': 'Fund flow',
  'transfer-finance': 'Transfers & investments',
  'tax-summary': 'Tax summary',
  'annual-outlook': 'Annual outlook',
  'cash-risk': 'Cash risk',
  subscriptions: 'Subscriptions',
  'merchant-concentration': 'Top merchants',
  'merchant-drift': 'Merchant changes',
}

const reportIdToGroup = new Map<string, ReportNavGroupKey>()
for (const group of REPORT_NAV_GROUPS) {
  for (const id of group.reportIds) {
    reportIdToGroup.set(id, group.key)
  }
}

export function reportNavGroupForId(reportId: string): ReportNavGroup | undefined {
  const key = reportIdToGroup.get(reportId)
  if (!key) return undefined
  return REPORT_NAV_GROUPS.find((g) => g.key === key)
}

export function reportNavGroupKey(reportId: string): ReportNavGroupKey | undefined {
  return reportIdToGroup.get(reportId)
}

export function menuOpenKeysForReportId(reportId: string): string[] {
  const groupKey = reportIdToGroup.get(reportId)
  if (!groupKey) return ['reports']
  return ['reports', groupKey]
}
