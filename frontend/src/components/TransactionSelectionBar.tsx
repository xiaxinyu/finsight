import { type ReactNode } from 'react'
import { CloseOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import { formatMoney } from '../utils/format'
import type { SelectionSummary } from '../utils/transactionSelection'

type Props = {
  count: number
  summary?: SelectionSummary
  disabled?: boolean
  onClear: () => void
  children: ReactNode
}

export function TransactionSelectionBar({ count, summary, disabled, onClear, children }: Props) {
  if (count <= 0) return null
  return (
    <div className="fs-tx-selection-bar" role="toolbar" aria-label="Batch actions">
      <div className="fs-tx-selection-meta">
        <span className="fs-tx-selection-count">{count}</span>
        <span className="fs-tx-selection-label">selected</span>
        {summary && (
          <span className="fs-tx-selection-totals">
            +{formatMoney(summary.income)} · −{formatMoney(summary.expense)} · net {formatMoney(summary.net)}
            {summary.dateFrom && summary.dateTo && (
              <> · {summary.dateFrom === summary.dateTo ? summary.dateFrom : `${summary.dateFrom} – ${summary.dateTo}`}</>
            )}
          </span>
        )}
      </div>
      <div className="fs-tx-selection-actions">{children}</div>
      <Button
        type="text"
        size="small"
        icon={<CloseOutlined />}
        disabled={disabled}
        onClick={onClear}
        className="fs-tx-selection-clear"
      >
        Clear
      </Button>
    </div>
  )
}
