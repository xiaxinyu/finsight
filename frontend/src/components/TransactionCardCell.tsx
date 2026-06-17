import { Tag, Tooltip } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { cellText } from '../utils/cell'
import { BANK_LABELS } from '../utils/statementDisplay'

function accountTail(row: TransactionRow): string {
  const raw = cellText(row.bankCardId) || cellText(row.bankCardName)
  if (!raw) return ''
  const digits = raw.replace(/\D/g, '')
  if (digits.length >= 4) return `···${digits.slice(-4)}`
  return raw.length > 8 ? `···${raw.slice(-4)}` : raw
}

export function TransactionCardCell({ row }: { row: TransactionRow }) {
  const bank = (row.bankCode || '').trim().toUpperCase()
  const typeName = cellText(row.cardTypeName) || cellText(row.cardTypeCode)
  const cardName = cellText(row.bankCardName)
  const tail = accountTail(row)
  const bankLabel = bank ? (BANK_LABELS[bank] || bank) : ''
  const tip = [bankLabel, typeName, cardName, tail, row.bankCardId ? `Card ${row.bankCardId}` : '']
    .filter(Boolean)
    .join(' · ')

  return (
    <Tooltip title={tip || undefined}>
      <span className="fs-tx-card-cell">
        {bank ? <Tag className="fs-tag fs-tx-bank-tag">{bank}</Tag> : null}
        <span className="fs-tx-card-type">{typeName || '—'}</span>
        {tail ? <span className="fs-tx-card-tail">{tail}</span> : null}
      </span>
    </Tooltip>
  )
}
