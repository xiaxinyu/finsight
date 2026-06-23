import { Typography } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { cellText } from '../utils/cell'
import { normalizeMerchantToken, rawMerchant } from '../utils/merchantNormalize'

type Props = {
  row: TransactionRow
}

export function TransactionMerchantCell({ row }: Props) {
  const raw = rawMerchant(row.opponentName, row.transactionDesc)
  const token = normalizeMerchantToken(raw)
  const label = cellText(raw) || token || '—'
  return (
    <Typography.Text className="fs-tx-merchant" ellipsis={{ tooltip: label }}>
      {label}
    </Typography.Text>
  )
}
