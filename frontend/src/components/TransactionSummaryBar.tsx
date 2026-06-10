import { finsightColors } from '../styles/finsight-tokens'
import { formatMoney } from '../utils/format'

export type TransactionSummaryBarProps = {
  total?: number
  income?: number
  expense?: number
  net?: number
  unclassified?: number
  transfers?: number
  truncated?: boolean
  loading?: boolean
  /** full = all metrics; toolbar = core 5 for filter bar */
  density?: 'full' | 'toolbar'
}

function StatItem({
  label,
  value,
  color,
}: {
  label: string
  value: string
  color?: string
}) {
  return (
    <span className="fs-tx-stat">
      <span className="fs-tx-stat-label">{label}</span>
      <span className="fs-tx-stat-value" style={color ? { color } : undefined}>{value}</span>
    </span>
  )
}

function Sep() {
  return <span className="fs-tx-stat-sep" aria-hidden />
}

function savingsRate(income: number, net: number): string {
  if (income <= 0) return '—'
  return `${((net / income) * 100).toFixed(1)}%`
}

export function TransactionSummaryBar({
  total = 0,
  income = 0,
  expense = 0,
  net = 0,
  unclassified = 0,
  transfers = 0,
  truncated = false,
  loading = false,
  density = 'full',
}: TransactionSummaryBarProps) {
  const busy = loading
  const netColor = net >= 0 ? finsightColors.income : finsightColors.expense
  const avgExpense = total > transfers ? expense / Math.max(1, total - transfers) : 0

  const allItems = [
    {
      key: 'count',
      label: truncated ? 'Txns (partial)' : 'Txns',
      value: busy ? '…' : String(total),
    },
    {
      key: 'income',
      label: 'Income',
      value: busy ? '…' : formatMoney(income),
      color: finsightColors.income,
    },
    {
      key: 'expense',
      label: 'Expense',
      value: busy ? '…' : formatMoney(expense),
      color: finsightColors.expense,
    },
    {
      key: 'net',
      label: 'Net',
      value: busy ? '…' : formatMoney(net),
      color: netColor,
    },
    {
      key: 'rate',
      label: 'Savings',
      value: busy ? '…' : savingsRate(income, net),
      color: net >= 0 ? finsightColors.income : finsightColors.expense,
    },
    {
      key: 'avg',
      label: 'Avg spend',
      value: busy ? '…' : formatMoney(avgExpense),
    },
    {
      key: 'xfer',
      label: 'Transfers',
      value: busy ? '…' : String(transfers),
    },
    {
      key: 'uncls',
      label: 'Unclassified',
      value: busy ? '…' : String(unclassified),
    },
  ]

  const toolbarKeys = new Set(['count', 'income', 'expense', 'net', 'uncls'])
  const items = density === 'toolbar'
    ? allItems.filter((item) => toolbarKeys.has(item.key))
    : allItems

  return (
    <div className={`fs-tx-summary-bar${density === 'toolbar' ? ' fs-tx-summary-bar--toolbar' : ''}`}>
      {items.map((item, idx) => (
        <span key={item.key} className="fs-tx-stat-group">
          {idx > 0 && <Sep />}
          <StatItem label={item.label} value={item.value} color={item.color} />
        </span>
      ))}
    </div>
  )
}
