import { Tooltip } from 'antd'
import { QuestionCircleOutlined } from '@ant-design/icons'
import type { ReactNode } from 'react'

type Props = {
  label: ReactNode
  hint: string
  className?: string
}

/** Compact KPI label with finance semantic explanation (v2.0.2). */
export function MetricExplanation({ label, hint, className }: Props) {
  return (
    <span className={`fs-metric-explanation ${className ?? ''}`.trim()}>
      {label}{' '}
      <Tooltip title={hint}>
        <QuestionCircleOutlined className="fs-metric-explanation-icon" aria-label="Metric explanation" />
      </Tooltip>
    </span>
  )
}

export const DASHBOARD_METRIC_HINTS = {
  realIncome: 'Salary and other report-role income. Excludes refunds, reimbursements, investment redemptions, and borrowing.',
  consumptionExpense: 'Living and budget-tracked spending. Excludes transfers, refunds, debt repayment, and investment purchases.',
  netCashflow: 'Real income minus consumption expense for the selected period.',
} as const

export const REPORT_METRIC_HINTS = {
  income: 'Income-direction transactions in the selected period. Internal transfers excluded. Refunds and reimbursements may appear until categorized with semantic roles.',
  expense: 'Expense-direction transactions in the period. Excludes transfers. Investment purchases and debt repayments may appear if not categorized.',
  net: 'Income minus expense for the filtered period and category scope.',
  compare: 'Period-over-period change using the same report filters.',
  consumptionSpend: 'Category expense totals in scope. Excludes transfers; investment and debt flows may appear if miscategorized.',
  budgetSpent: 'Actual spending against budget buckets for the selected period (consumption scope).',
  budgetLimit: 'Configured limits per budget bucket for the selected period.',
  budgetUtilization: 'Spent divided by budget limit. High utilization may leave little room before month-end.',
  spendingDriftCompare: 'Same category and card filters on both periods. Totals use expense-direction semantics.',
  monthlyPace: 'Spending normalized to a 30-day month so uneven date ranges compare fairly.',
  forecastIncome: 'Projected inflows from the hybrid forecast model for the selected scenario.',
  forecastExpense: 'Projected spending from historical patterns and scenario adjustments.',
  forecastNet: 'Forecast income minus forecast expense for the scenario year.',
  deficitMonths: 'Months where projected net cashflow is negative in the forecast scenario.',
  trendExpenseDelta: 'Year-over-year change in total expense using the same semantic expense scope.',
  savingsRateDelta: 'Change in savings rate (net ÷ income) between comparison years, in percentage points.',
  cashRiskDeficit: 'Months in the forecast where projected net cashflow is negative.',
  cashRiskHighDays: 'Days flagged high when projected outflows exceed inflows and safety buffer.',
  fixedShare: 'Share of spending classified as fixed or essential budget behavior.',
  variableShare: 'Share of variable or discretionary consumption spending.',
} as const
