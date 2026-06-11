import { type ReactNode } from 'react'
import { CloseOutlined } from '@ant-design/icons'
import { Button } from 'antd'

type Props = {
  count: number
  disabled?: boolean
  onClear: () => void
  children: ReactNode
}

export function TransactionSelectionBar({ count, disabled, onClear, children }: Props) {
  if (count <= 0) return null
  return (
    <div className="fs-tx-selection-bar" role="toolbar" aria-label="Batch actions">
      <div className="fs-tx-selection-meta">
        <span className="fs-tx-selection-count">{count}</span>
        <span className="fs-tx-selection-label">selected</span>
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
