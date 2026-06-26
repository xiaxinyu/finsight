import { Space, Tag, Tooltip, Typography } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { cellText } from '../utils/cell'
import { buildTransactionDisplay } from '../utils/transactionDisplay'
import { transactionDisplayTags } from '../utils/transactionDisplayTags'

export function TransactionTypeBadge({ kind }: { kind: string }) {
  if (kind === 'transfer') {
    return <span className="fs-tx-type fs-tx-type--transfer">Transfer</span>
  }
  if (kind === 'income') {
    return <span className="fs-tx-type fs-tx-type--income">Income</span>
  }
  return <span className="fs-tx-type fs-tx-type--expense">Expense</span>
}

type Props = {
  row: TransactionRow
  showTags?: boolean
}

export function TransactionLedgerCell({ row, showTags = true }: Props) {
  const { title, subtitle, tooltip } = buildTransactionDisplay(row)
  const category = cellText(row.consumeName)
  const tags = transactionDisplayTags(row).slice(0, 3)

  return (
    <div className="fs-tx-ledger-cell">
      <Tooltip title={tooltip !== title ? tooltip : undefined}>
        <Typography.Text strong className="fs-tx-ledger-title" ellipsis>
          {title}
        </Typography.Text>
      </Tooltip>
      {subtitle ? (
        <Typography.Text type="secondary" className="fs-tx-ledger-meta" ellipsis={{ tooltip: subtitle }}>
          {subtitle}
        </Typography.Text>
      ) : null}
      {showTags && (category || tags.length > 0) && (
        <Space size={4} wrap className="fs-tx-ledger-tags">
          {category && (
            <Tag bordered={false} className="fs-tx-ledger-tag">{category}</Tag>
          )}
          {tags.map((tag) => (
            <Tooltip key={tag.id} title={tag.hint}>
              <Tag bordered={false} color={tag.color || 'default'} className="fs-tx-ledger-tag">{tag.label}</Tag>
            </Tooltip>
          ))}
        </Space>
      )}
    </div>
  )
}
