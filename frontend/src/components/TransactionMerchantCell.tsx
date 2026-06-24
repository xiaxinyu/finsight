import { Typography } from 'antd'
import type { TransactionRow } from '../api/transaction'
import { buildTransactionDisplay } from '../utils/transactionDisplay'

type Props = {
  row: TransactionRow
}

export function TransactionMerchantCell({ row }: Props) {
  const { title, tooltip } = buildTransactionDisplay(row)
  return (
    <Typography.Text className="fs-tx-merchant" ellipsis={{ tooltip: tooltip || title }}>
      {title}
    </Typography.Text>
  )
}
