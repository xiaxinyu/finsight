import { Space, Tag, Tooltip } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { cellText } from '../utils/cell'
import { detectTransactionRiskTags, isUnclassifiedRow, riskTagMeta } from '../utils/transactionRisk'

type Props = {
  row: TransactionRow
  pageMaxAmount?: number
}

export function TransactionCategoryCell({ row, pageMaxAmount = 0 }: Props) {
  const category = cellText(row.consumeName) || cellText(row.consumeCode)
  const unclassified = isUnclassifiedRow(row)
  const riskTags = detectTransactionRiskTags(row, { amountMax: pageMaxAmount })
    .filter((t) => t !== 'unclassified' && t !== 'anomaly')

  return (
    <div className="fs-tx-category-cell">
      <div className="fs-tx-category-name">
        {category || <span className="fs-tx-category-empty">—</span>}
      </div>
      <Space size={4} wrap className="fs-tx-category-tags">
        {unclassified && (
          <Tag bordered={false} color="gold" className="fs-tx-risk-tag">Unclassified</Tag>
        )}
        {riskTags.map((tag) => {
          const meta = riskTagMeta(tag)
          return (
            <Tooltip key={tag} title={meta.hint}>
              <Tag bordered={false} color={meta.color} className="fs-tx-risk-tag">{meta.label}</Tag>
            </Tooltip>
          )
        })}
      </Space>
    </div>
  )
}
