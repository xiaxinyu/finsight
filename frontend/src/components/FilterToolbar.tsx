import { type FormEvent, type KeyboardEvent, type ReactNode } from 'react'
import { Button, Space } from 'antd'

type Props = {
  children: ReactNode
  loading?: boolean
  onApply: () => void
  applyLabel?: string
  actions?: ReactNode
}

export function FilterToolbar({ children, loading = false, onApply, applyLabel = 'Apply', actions }: Props) {
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
          <Button type="primary" htmlType="submit" size="small" loading={loading} disabled={loading}>
            {loading ? 'Loading…' : applyLabel}
          </Button>
        </Space>
        {actions && <div className="fs-toolbar-actions">{actions}</div>}
      </form>
    </div>
  )
}
