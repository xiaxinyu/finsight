import { Tag, Tooltip } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { cellText } from '../utils/cell'
import { BANK_LABELS } from '../utils/statementDisplay'

export function TransactionCardCell({ row }: { row: TransactionRow }) {
  const bank = (row.bankCode || '').trim().toUpperCase()
  const typeName = cellText(row.cardTypeName) || cellText(row.cardTypeCode)
  const cardName = cellText(row.bankCardName)
  const bankLabel = bank ? (BANK_LABELS[bank] || bank) : ''
  const tip = [bankLabel, typeName, cardName, row.bankCardId ? `Card ${row.bankCardId}` : '']
    .filter(Boolean)
    .join(' · ')

  return (
    <Tooltip title={tip || undefined}>
      <span className="fs-tx-card-cell">
        {bank ? <Tag className="fs-tag fs-tx-bank-tag">{bank}</Tag> : null}
        <span className="fs-tx-card-type">{typeName || '—'}</span>
      </span>
    </Tooltip>
  )
}
