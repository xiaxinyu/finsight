export type ReportConfig = {
  title: string
  subtitle?: string
  type: 'categoryBar' | 'incomeVsExpense' | 'yearCompare' | 'weekSummary' | 'monthlyCompare' | 'timeCurve'
  endpoint?: string
  txnType?: 'income' | 'expense'
  chartKind?: 'bar' | 'donut'
  dateRange?: boolean
  compareYear?: boolean
  chartProfile?: string
}

export const reportConfigs: Record<string, ReportConfig> = {
  'transaction-trend': { title: 'Transaction Trend', subtitle: 'Expense flow over time by category', type: 'categoryBar', endpoint: '/transaction-report/consume', txnType: 'expense', dateRange: true, chartProfile: 'categoryBar' },
  'category-breakdown': { title: 'Category Breakdown', subtitle: 'Share of spending by category', type: 'categoryBar', endpoint: '/transaction-report/consume', txnType: 'expense', dateRange: true, chartKind: 'donut', chartProfile: 'donut' },
  'category-comparison': { title: 'Category Comparison', subtitle: 'Year-over-year category mix', type: 'yearCompare', txnType: 'expense', compareYear: true, chartProfile: 'donut' },
  'weekly-summary': { title: 'Weekly Summary', subtitle: 'Spending pattern by day of week', type: 'weekSummary', txnType: 'expense', dateRange: true, chartProfile: 'categoryBar' },
  'monthly-comparison': { title: 'Monthly Comparison', subtitle: 'Month-by-month expense totals', type: 'monthlyCompare', txnType: 'expense', chartProfile: 'timeSeries' },
  'income-vs-expense': { title: 'Income vs Expense', subtitle: 'Monthly cash flow and surplus', type: 'incomeVsExpense', chartProfile: 'compareBars' },
  'income-curve': { title: 'Income Curve', subtitle: 'Monthly income trend', type: 'timeCurve', txnType: 'income', chartProfile: 'timeSeries' },
  'expense-curve': { title: 'Expense Curve', subtitle: 'Monthly expense trend', type: 'timeCurve', txnType: 'expense', chartProfile: 'timeSeries' },
}

export const reportIds = Object.keys(reportConfigs)
