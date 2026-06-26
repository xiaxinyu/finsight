import { Space, Tag, Tooltip } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { cellText } from '../utils/cell'
import { transactionDisplayTags } from '../utils/transactionDisplayTags'

type Props = {
  row: TransactionRow
}

export function TransactionCategoryCell({ row }: Props) {
  const category = cellText(row.consumeName) || cellText(row.consumeCode)
  const tags = transactionDisplayTags(row)

  return (
    <div className="fs-tx-category-cell">
      <div className="fs-tx-category-name">
        {category || <span className="fs-tx-category-empty">—</span>}
      </div>
      {tags.length > 0 && (
        <Space size={4} wrap className="fs-tx-category-tags">
          {tags.map((tag) => (
            <Tooltip key={tag.id} title={tag.hint}>
              <Tag bordered={false} color={tag.color || 'default'} className="fs-tx-semantic-tag">
                {tag.label}
              </Tag>
            </Tooltip>
          ))}
        </Space>
      )}
    </div>
  )
}
