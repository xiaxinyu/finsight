import { type FormEvent, type KeyboardEvent, type ReactNode } from 'react'
import { Badge, Button, Space, Tag } from 'antd'

type Props = {
  children: ReactNode
  loading?: boolean
  onApply: () => void
  applyLabel?: string
  actions?: ReactNode
  dirty?: boolean
  selectedCount?: number
}

export function FilterToolbar({
  children,
  loading = false,
  onApply,
  applyLabel = 'Apply',
  actions,
  dirty = false,
  selectedCount,
}: Props) {
  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !loading) {
      e.preventDefault()
      onApply()
    }
  }

  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (!loading) onApply()
  }

  return (
    <div className="fs-toolbar-bar">
      <form onSubmit={onSubmit} onKeyDown={onKeyDown} className="fs-toolbar-form">
        <Space wrap size="small" className={loading ? 'fs-filter-disabled' : undefined}>
          {children}
          <Badge dot={dirty && !loading}>
            <Button type="primary" htmlType="submit" size="small" loading={loading} disabled={loading}>
              {loading ? 'Loading…' : applyLabel}
            </Button>
          </Badge>
          {dirty && !loading && <Tag className="fs-filter-dirty-tag">Filters changed</Tag>}
        </Space>
        <div className="fs-toolbar-actions">
          {selectedCount != null && selectedCount > 0 && (
            <Tag color="blue">{selectedCount} selected</Tag>
          )}
          {actions}
        </div>
      </form>
    </div>
  )
}
