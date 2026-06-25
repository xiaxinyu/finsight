import { CloseOutlined } from '@ant-design/icons'
import { Tag } from 'antd'
import { formatMoney } from '../utils/format'

export type ActiveFilterChip = {
  key: string
  label: string
  onRemove: () => void
}

export type FilterImpact = {
  total?: number
  income?: number
  expense?: number
  net?: number
  loading?: boolean
}

type Props = {
  chips: ActiveFilterChip[]
  impact?: FilterImpact
  onClearAll?: () => void
  showClearAll?: boolean
}

export function TransactionActiveFilters({ chips, impact, onClearAll, showClearAll = false }: Props) {
  if (chips.length === 0 && !showClearAll) return null
  const busy = impact?.loading
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
      {impact && (
        <span className="fs-tx-filter-impact">
          {busy ? '…' : (
            <>
              {(impact.total ?? 0).toLocaleString()} txns
              {' · '}
              <span className="fs-tx-filter-impact-income">+{formatMoney(impact.income ?? 0)}</span>
              {' · '}
              <span className="fs-tx-filter-impact-expense">−{formatMoney(impact.expense ?? 0)}</span>
              {' · '}
              net {formatMoney(impact.net ?? 0)}
            </>
          )}
        </span>
      )}
      {onClearAll && (showClearAll || chips.length > 0) && (
        <button type="button" className="fs-tx-clear-filters" onClick={onClearAll}>
          Clear all
        </button>
      )}
    </div>
  )
}
