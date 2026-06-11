import { CloseOutlined } from '@ant-design/icons'
import { Tag } from 'antd'

export type ActiveFilterChip = {
  key: string
  label: string
  onRemove: () => void
}

type Props = {
  chips: ActiveFilterChip[]
  onClearAll?: () => void
}

export function TransactionActiveFilters({ chips, onClearAll }: Props) {
  if (chips.length === 0) return null
  return (
    <div className="fs-tx-active-filters">
      <span className="fs-tx-active-filters-label">Active</span>
      {chips.map((chip) => (
        <Tag
          key={chip.key}
          className="fs-tx-filter-chip"
          closable
          closeIcon={<CloseOutlined />}
          onClose={(e) => {
            e.preventDefault()
            chip.onRemove()
          }}
        >
          {chip.label}
        </Tag>
      ))}
      {chips.length > 1 && onClearAll && (
        <button type="button" className="fs-tx-clear-filters" onClick={onClearAll}>
          Clear all
        </button>
      )}
    </div>
  )
}
