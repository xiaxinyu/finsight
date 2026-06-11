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
  /** cards = grid tiles; compact = single-line strip (max table space) */
  variant?: 'cards' | 'compact'
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

function InlineStat({
  label,
  value,
  tone,
  active,
  onClick,
  title,
}: {
  label: string
  value: string
  tone?: 'income' | 'expense' | 'warn' | 'default'
  active?: boolean
  onClick?: () => void
  title?: string
}) {
  const Tag = onClick ? 'button' : 'span'
  return (
    <Tag
      type={onClick ? 'button' : undefined}
      className={`fs-tx-kpi-inline${tone && tone !== 'default' ? ` fs-tx-kpi-inline--${tone}` : ''}${active ? ' fs-tx-kpi-inline--active' : ''}${onClick ? ' fs-tx-kpi-inline--clickable' : ''}`}
      onClick={onClick}
      title={title}
    >
      <span className="fs-tx-kpi-inline-value">{value}</span>
      <span className="fs-tx-kpi-inline-label">{label}</span>
    </Tag>
  )
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
  variant = 'compact',
}: TransactionKpiStripProps) {
  const busy = loading
  const netTone = net >= 0 ? 'income' : 'expense'
  const avgExpense = total > transfers ? expense / Math.max(1, total - transfers) : 0

  if (variant === 'compact') {
    return (
      <div className="fs-tx-kpi-strip fs-tx-kpi-strip--compact">
        <InlineStat
          label={truncated ? 'Txns*' : 'Txns'}
          value={busy ? '…' : String(total)}
          title={truncated ? 'Partial count for current filters' : undefined}
        />
        <span className="fs-tx-kpi-inline-sep" aria-hidden />
        <InlineStat label="In" value={busy ? '…' : formatMoney(income)} tone="income" title="Income" />
        <span className="fs-tx-kpi-inline-sep" aria-hidden />
        <InlineStat label="Out" value={busy ? '…' : formatMoney(expense)} tone="expense" title="Expense" />
        <span className="fs-tx-kpi-inline-sep" aria-hidden />
        <InlineStat label="Net" value={busy ? '…' : formatMoney(net)} tone={netTone} />
        <span className="fs-tx-kpi-inline-sep" aria-hidden />
        <InlineStat
          label="Save"
          value={busy ? '…' : savingsRate(income, net)}
          tone={netTone}
          title="Savings rate"
        />
        <span className="fs-tx-kpi-inline-sep" aria-hidden />
        <InlineStat
          label="Avg"
          value={busy ? '…' : formatMoney(avgExpense)}
          title="Avg spend per non-transfer txn"
        />
        <span className="fs-tx-kpi-inline-sep" aria-hidden />
        <InlineStat
          label="Uncls"
          value={busy ? '…' : String(unclassified)}
          tone={unclassified > 0 ? 'warn' : 'default'}
          active={unclassifiedActive}
          onClick={onUnclassifiedClick}
          title="Unclassified — click to filter"
        />
      </div>
    )
  }

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
