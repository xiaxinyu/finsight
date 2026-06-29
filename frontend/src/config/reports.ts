export type ReportConfig = {
  title: string
  subtitle?: string
  type: 'categoryBar' | 'incomeVsExpense' | 'yearCompare' | 'weekSummary' | 'monthlyCompare' | 'timeCurve' | 'billsCalendar' | 'homeBuckets' | 'budgetVsActual' | 'transfers' | 'annualOutlook' | 'trendChanges' | 'cashRisk' | 'merchantSubscriptions' | 'merchantConcentration' | 'merchantDrift'
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
    type: 'budgetVsActual',
    chartKind: 'bar',
    chartProfile: 'compareBars',
    dateRange: true,
    legacyIds: ['category-breakdown', 'transaction-trend'],
  },
  'fund-flow': {
    title: 'Fund Flow',
    subtitle: 'Internal transfers excluded from spending reports',
    type: 'transfers',
    dateRange: true,
  },
  'fixed-vs-variable': {
    title: 'Fixed vs Variable',
    subtitle: 'Semantic expense structure — Dining, Medical, Housing, and fixed costs',
    type: 'homeBuckets',
    chartProfile: 'categoryBar',
    legacyIds: ['weekly-summary'],
  },
  'spending-drift': {
    title: 'Spending Drift',
    subtitle: 'Compare semantic buckets between two periods — stable even when categories move',
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
  'annual-outlook': {
    title: 'Annual Outlook',
    subtitle: 'Statistical income/expense forecast with scenario bands',
    type: 'annualOutlook',
  },
  'trend-changes': {
    title: 'Trend Changes',
    subtitle: 'Category growth and savings inflection points',
    type: 'trendChanges',
    compareYear: true,
  },
  'cash-risk': {
    title: 'Cash Risk',
    subtitle: 'Projected deficit months and liquidity pressure',
    type: 'cashRisk',
  },
  subscriptions: {
    title: 'Subscriptions',
    subtitle: 'Recurring charges and optimizable spend',
    type: 'merchantSubscriptions',
  },
  'merchant-concentration': {
    title: 'Merchant Concentration',
    subtitle: 'Where spending clusters across merchants',
    type: 'merchantConcentration',
  },
  'merchant-drift': {
    title: 'Merchant Drift',
    subtitle: 'Year-over-year merchant spend movers',
    type: 'merchantDrift',
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
