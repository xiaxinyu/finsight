export type ReportConfig = {
  title: string
  subtitle?: string
  type: 'categoryBar' | 'incomeVsExpense' | 'yearCompare' | 'weekSummary' | 'monthlyCompare' | 'timeCurve' | 'billsCalendar' | 'homeBuckets'
  endpoint?: string
  txnType?: 'income' | 'expense'
  chartKind?: 'bar' | 'donut'
  dateRange?: boolean
  compareYear?: boolean
  chartProfile?: string
  legacyIds?: string[]
}

/** Five decision-oriented reports (replaces eight overlapping retrospective views). */
export const reportConfigs: Record<string, ReportConfig> = {
  cashflow: {
    title: 'Cashflow',
    subtitle: 'Monthly income, expense, and surplus trend',
    type: 'incomeVsExpense',
    chartProfile: 'compareBars',
    legacyIds: ['income-vs-expense', 'monthly-comparison', 'income-curve', 'expense-curve'],
  },
  'budget-vs-actual': {
    title: 'Budget vs Actual',
    subtitle: 'Which categories are over or under plan',
    type: 'categoryBar',
    endpoint: '/transaction-report/consume',
    txnType: 'expense',
    dateRange: true,
    chartKind: 'donut',
    chartProfile: 'donut',
    legacyIds: ['category-breakdown', 'transaction-trend'],
  },
  'fixed-vs-variable': {
    title: 'Fixed vs Variable',
    subtitle: 'Fixed burden and weekday spending patterns',
    type: 'homeBuckets',
    chartProfile: 'categoryBar',
    legacyIds: ['weekly-summary'],
  },
  'spending-drift': {
    title: 'Spending Drift',
    subtitle: 'Year-over-year category changes',
    type: 'yearCompare',
    txnType: 'expense',
    compareYear: true,
    chartProfile: 'donut',
    legacyIds: ['category-comparison'],
  },
  'bills-calendar': {
    title: 'Bills Calendar',
    subtitle: 'Upcoming fixed payments in the next 30 days',
    type: 'billsCalendar',
  },
}

export const reportIds = Object.keys(reportConfigs)

/** Map legacy report URLs to new decision reports. */
export const legacyReportRedirects: Record<string, string> = {}
for (const [id, cfg] of Object.entries(reportConfigs)) {
  for (const legacy of cfg.legacyIds || []) {
    legacyReportRedirects[legacy] = id
  }
}
