export type ReportConfig = {
  title: string
  subtitle?: string
  type: 'categoryBar' | 'incomeVsExpense' | 'yearCompare' | 'weekSummary' | 'monthlyCompare' | 'timeCurve' | 'billsCalendar' | 'homeBuckets' | 'budgetVsActual' | 'transfers' | 'annualOutlook' | 'trendChanges' | 'debtTrends' | 'incomeTrends' | 'cashRisk' | 'merchantSubscriptions' | 'merchantConcentration' | 'merchantDrift' | 'semanticScope'
  endpoint?: string
  txnType?: 'income' | 'expense'
  semanticScope?: 'expense' | 'income' | 'non_pnl' | 'tax' | 'refund' | 'all'
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
    subtitle: 'Internal transfer pairs excluded from spending reports',
    type: 'transfers',
    dateRange: true,
  },
  'transfer-finance': {
    title: 'Transfers & Investments',
    subtitle: 'Account transfers, loans, and investments — excluded from spending',
    type: 'semanticScope',
    semanticScope: 'non_pnl',
    chartProfile: 'donut',
    dateRange: true,
  },
  'tax-summary': {
    title: 'Tax Summary',
    subtitle: 'Tax paid and refunds — tracked separately from daily spending',
    type: 'semanticScope',
    semanticScope: 'tax',
    chartProfile: 'donut',
    dateRange: true,
  },
  'fixed-vs-variable': {
    title: 'Fixed vs Variable',
    subtitle: 'Semantic expense structure — Dining, Medical, Housing, and fixed costs',
    type: 'homeBuckets',
    chartProfile: 'donut',
    legacyIds: ['weekly-summary'],
  },
  'spending-drift': {
    title: 'Period Comparison',
    subtitle: 'Compare spending between any two date ranges by classification',
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
    title: 'Consumption Trends',
    subtitle: 'Year-over-year living spend — totals, categories, and drill-down',
    type: 'trendChanges',
    compareYear: true,
  },
  'debt-trends': {
    title: 'Debt Trends',
    subtitle: 'Outstanding balance, yearly change, and borrowing vs repayments',
    type: 'debtTrends',
    compareYear: true,
  },
  'income-trends': {
    title: 'Income Trends',
    subtitle: 'Year-over-year income — totals, sources, and drill-down',
    type: 'incomeTrends',
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
    title: 'Top Merchants',
    subtitle: 'Where spending clusters across merchants',
    type: 'merchantConcentration',
  },
  'merchant-drift': {
    title: 'Merchant Changes',
    subtitle: 'Year-over-year merchant spending movers',
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
