import { Space, Tag, Tooltip, Typography } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { cellText } from '../utils/cell'
import { detectTransactionRiskTags, riskTagMeta } from '../utils/transactionRisk'

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
  pageMaxAmount?: number
  showTags?: boolean
}

export function TransactionLedgerCell({ row, pageMaxAmount = 0, showTags = true }: Props) {
  const desc = cellText(row.transactionDesc) || '—'
  const memo = cellText(row.demoArea)
  const category = cellText(row.consumeName)
  const tags = detectTransactionRiskTags(row, { amountMax: pageMaxAmount })
    .slice(0, 3)

  return (
    <div className="fs-tx-ledger-cell">
      <Typography.Text strong className="fs-tx-ledger-title" ellipsis={{ tooltip: desc }}>
        {desc}
      </Typography.Text>
      {memo ? (
        <Typography.Text type="secondary" className="fs-tx-ledger-meta" ellipsis={{ tooltip: memo }}>
          {memo}
        </Typography.Text>
      ) : null}
      {showTags && (category || tags.length > 0) && (
        <Space size={4} wrap className="fs-tx-ledger-tags">
          {category && (
            <Tag bordered={false} className="fs-tx-ledger-tag">{category}</Tag>
          )}
          {tags.map((tag) => {
            const meta = riskTagMeta(tag)
            return (
              <Tooltip key={tag} title={meta.hint}>
                <Tag bordered={false} color={meta.color} className="fs-tx-ledger-tag">{meta.label}</Tag>
              </Tooltip>
            )
          })}
        </Space>
      )}
    </div>
  )
}
