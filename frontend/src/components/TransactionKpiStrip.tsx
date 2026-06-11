import { formatMoney } from '../utils/format'

export type TransactionKpiStripProps = {
  total?: number
  income?: number
  expense?: number
  net?: number
  unclassified?: number
  transfers?: number
  truncated?: boolean
  loading?: boolean
  onUnclassifiedClick?: () => void
  unclassifiedActive?: boolean
}

type KpiCardProps = {
  label: string
  value: string
  tone?: 'default' | 'income' | 'expense' | 'net' | 'warn'
  hint?: string
  active?: boolean
  onClick?: () => void
}

function KpiCard({ label, value, tone = 'default', hint, active, onClick }: KpiCardProps) {
  const Tag = onClick ? 'button' : 'div'
  return (
    <Tag
      type={onClick ? 'button' : undefined}
      className={`fs-tx-kpi-card fs-tx-kpi-card--${tone}${active ? ' fs-tx-kpi-card--active' : ''}${onClick ? ' fs-tx-kpi-card--clickable' : ''}`}
      onClick={onClick}
      title={hint}
    >
      <span className="fs-tx-kpi-value">{value}</span>
      <span className="fs-tx-kpi-label">{label}</span>
    </Tag>
  )
}

function savingsRate(income: number, net: number): string {
  if (income <= 0) return '—'
  return `${((net / income) * 100).toFixed(1)}%`
}

export function TransactionKpiStrip({
  total = 0,
  income = 0,
  expense = 0,
  net = 0,
  unclassified = 0,
  transfers = 0,
  truncated = false,
  loading = false,
  onUnclassifiedClick,
  unclassifiedActive = false,
}: TransactionKpiStripProps) {
  const busy = loading
  const netTone = net >= 0 ? 'income' : 'expense'
  const avgExpense = total > transfers ? expense / Math.max(1, total - transfers) : 0

  return (
    <div className="fs-tx-kpi-strip">
      <KpiCard
        label={truncated ? 'Transactions (partial)' : 'Transactions'}
        value={busy ? '…' : String(total)}
      />
      <KpiCard
        label="Income"
        tone="income"
        value={busy ? '…' : formatMoney(income)}
      />
      <KpiCard
        label="Expense"
        tone="expense"
        value={busy ? '…' : formatMoney(expense)}
      />
      <KpiCard
        label="Net"
        tone={netTone}
        value={busy ? '…' : formatMoney(net)}
      />
      <KpiCard
        label="Savings rate"
        tone={netTone}
        value={busy ? '…' : savingsRate(income, net)}
        hint="Net ÷ income in selected period"
      />
      <KpiCard
        label="Avg spend"
        value={busy ? '…' : formatMoney(avgExpense)}
        hint="Expense ÷ non-transfer transactions"
      />
      <KpiCard
        label="Unclassified"
        tone={unclassified > 0 ? 'warn' : 'default'}
        value={busy ? '…' : String(unclassified)}
        active={unclassifiedActive}
        onClick={onUnclassifiedClick}
        hint={onUnclassifiedClick ? 'Click to filter unclassified rows' : undefined}
      />
    </div>
  )
}
