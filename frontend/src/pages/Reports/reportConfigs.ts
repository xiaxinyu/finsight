export type ReportConfig = {
  title: string
  type: 'categoryBar' | 'incomeVsExpense' | 'yearCompare' | 'weekSummary' | 'monthlyCompare' | 'timeCurve'
  endpoint?: string
  txnType?: 'income' | 'expense'
  chartKind?: 'bar' | 'donut'
  dateRange?: boolean
  compareYear?: boolean
  chartProfile?: string
}

export const reportConfigs: Record<string, ReportConfig> = {
  'transaction-trend': { title: 'Transaction Trend', type: 'categoryBar', endpoint: '/transaction-report/consume', txnType: 'expense', dateRange: true, chartProfile: 'categoryBar' },
  'category-breakdown': { title: 'Category Breakdown', type: 'categoryBar', endpoint: '/transaction-report/consume', txnType: 'expense', dateRange: true, chartKind: 'donut', chartProfile: 'donut' },
  'category-comparison': { title: 'Category Comparison', type: 'yearCompare', txnType: 'expense', compareYear: true, chartProfile: 'donut' },
  'weekly-summary': { title: 'Weekly Summary', type: 'weekSummary', txnType: 'expense', dateRange: true, chartProfile: 'categoryBar' },
  'monthly-comparison': { title: 'Monthly Comparison', type: 'monthlyCompare', txnType: 'expense', chartProfile: 'timeSeries' },
  'income-vs-expense': { title: 'Income vs Expense', type: 'incomeVsExpense', chartProfile: 'compareBars' },
  'income-curve': { title: 'Income Curve', type: 'timeCurve', txnType: 'income', chartProfile: 'timeSeries' },
  'expense-curve': { title: 'Expense Curve', type: 'timeCurve', txnType: 'expense', chartProfile: 'timeSeries' },
}
