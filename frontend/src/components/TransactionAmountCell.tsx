import { Tooltip } from 'antd'
import { ArrowDownOutlined, ArrowUpOutlined, SwapOutlined } from '@ant-design/icons'
import type { TransactionRow } from '../api/transaction'
import { formatNumber } from '../utils/format'
import { finsightColors } from '../styles/finsight-tokens'
import { isAnomalyAmount } from '../utils/transactionDisplayTags'
import { amountIntensity } from '../utils/transactionRisk'
import { rowAmount, rowTxnKind } from '../utils/transactionAmount'

type Props = {
  row: TransactionRow
  pageMaxAmount?: number
  /** Relative amount bar behind the figure (off by default for classic ledger layout). */
  showBar?: boolean
}

export function TransactionAmountCell({ row, pageMaxAmount = 0, showBar = true }: Props) {
  const kind = row.txnKind || rowTxnKind(row)
  const amount = rowAmount(row)
  const intensity = amountIntensity(amount, pageMaxAmount)
  const isAnomaly = isAnomalyAmount(amount, pageMaxAmount)

  const color = kind === 'income'
    ? finsightColors.income
    : kind === 'transfer'
      ? '#64748b'
      : finsightColors.expense

  const prefix = kind === 'income' ? '+' : kind === 'transfer' ? '' : '−'
  const icon = kind === 'income'
    ? <ArrowUpOutlined />
    : kind === 'transfer'
      ? <SwapOutlined />
      : <ArrowDownOutlined />

  return (
    <div className="fs-tx-amount-cell">
      {showBar && pageMaxAmount > 0 && (
        <div className="fs-tx-amount-bar" aria-hidden>
          <div
            className={`fs-tx-amount-bar-fill fs-tx-amount-bar-fill--${kind}`}
            style={{ width: `${intensity}%` }}
          />
        </div>
      )}
      <div className="fs-tx-amount-line">
        <span className={`fs-tx-amount-icon fs-tx-amount-icon--${kind}`} aria-hidden>{icon}</span>
        <Tooltip title={isAnomaly ? 'Larger than most rows on this page' : undefined}>
          <span
            className={`fs-tx-amount-value${isAnomaly ? ' fs-tx-amount-value--anomaly' : ''}`}
            style={{ color }}
          >
            {prefix}{formatNumber(amount)}
          </span>
        </Tooltip>
      </div>
    </div>
  )
}
