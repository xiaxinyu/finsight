import { Typography } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { cellText } from '../utils/cell'

export function TransactionTypeBadge({ kind }: { kind: string }) {
  if (kind === 'transfer') {
    return <span className="fs-tx-type fs-tx-type--transfer">Transfer</span>
  }
  if (kind === 'income') {
    return <span className="fs-tx-type fs-tx-type--income">Income</span>
  }
  return <span className="fs-tx-type fs-tx-type--expense">Expense</span>
}

export function TransactionLedgerCell({ row }: { row: TransactionRow }) {
  const desc = cellText(row.transactionDesc) || '—'
  const category = cellText(row.consumeName)
  const memo = cellText(row.demoArea)
  const meta = [category, memo].filter(Boolean).join(' · ')

  return (
    <div className="fs-tx-ledger-cell">
      <Typography.Text strong className="fs-tx-ledger-title" ellipsis={{ tooltip: desc }}>
        {desc}
      </Typography.Text>
      {meta ? (
        <Typography.Text type="secondary" className="fs-tx-ledger-meta" ellipsis={{ tooltip: meta }}>
          {meta}
        </Typography.Text>
      ) : (
        <Typography.Text type="secondary" className="fs-tx-ledger-meta fs-tx-ledger-meta--empty">
          No category
        </Typography.Text>
      )}
    </div>
  )
}
