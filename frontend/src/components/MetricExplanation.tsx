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
} as const
