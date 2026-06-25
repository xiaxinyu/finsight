import { type FormEvent, type KeyboardEvent, type ReactNode } from 'react'
import { Badge, Button, Space, Tag } from 'antd'

type Props = {
  children: ReactNode
  loading?: boolean
  onApply: () => void
  applyLabel?: string
  actions?: ReactNode
  summary?: ReactNode
  dirty?: boolean
  selectedCount?: number
  onReset?: () => void
  resetLabel?: string
  canReset?: boolean
}

export function FilterToolbar({
  children,
  loading = false,
  onApply,
  applyLabel = 'Apply',
  actions,
  summary,
  dirty = false,
  selectedCount,
  onReset,
  resetLabel = 'Reset',
  canReset = false,
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
    <div className={`fs-toolbar-bar${summary ? ' fs-toolbar-bar--with-summary' : ''}`}>
      <form
        onSubmit={onSubmit}
        onKeyDown={onKeyDown}
        className={`fs-toolbar-form${summary ? ' fs-toolbar-form--with-summary' : ''}`}
      >
        <div className="fs-toolbar-filters">
          <Space wrap size="small" className={loading ? 'fs-filter-disabled' : undefined}>
            {children}
          </Space>
        </div>
        {summary ? <div className="fs-toolbar-summary">{summary}</div> : null}
        <div className="fs-toolbar-actions">
          {canReset && onReset && (
            <Button size="small" disabled={loading} onClick={onReset}>
              {resetLabel}
            </Button>
          )}
          <Badge dot={dirty && !loading}>
            <Button type="primary" htmlType="submit" size="small" loading={loading} disabled={loading}>
              {loading ? 'Loading…' : applyLabel}
            </Button>
          </Badge>
          {dirty && !loading && <Tag className="fs-filter-dirty-tag">Filters changed</Tag>}
          {selectedCount != null && selectedCount > 0 && (
            <Tag color="blue">{selectedCount} selected</Tag>
          )}
          {actions}
        </div>
      </form>
    </div>
  )
}
