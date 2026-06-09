import { type FormEvent, type KeyboardEvent, type ReactNode } from 'react'
import { Button, Space } from 'antd'
import { ContentCard } from './ContentCard'

type Props = {
  children: ReactNode
  loading?: boolean
  onApply: () => void
  applyLabel?: string
}

export function FilterToolbar({ children, loading = false, onApply, applyLabel = 'Apply' }: Props) {
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
    <ContentCard size="small" className="fs-filter-toolbar">
      <form onSubmit={onSubmit} onKeyDown={onKeyDown}>
        <Space wrap size="middle" className={loading ? 'fs-filter-disabled' : undefined}>
          {children}
          <Button type="primary" htmlType="submit" loading={loading} disabled={loading}>
            {loading ? 'Loading…' : applyLabel}
          </Button>
        </Space>
      </form>
    </ContentCard>
  )
}
