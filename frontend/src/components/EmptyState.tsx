import type { ReactNode } from 'react'
import { Typography } from 'antd'
import { InboxOutlined } from '@ant-design/icons'

type Props = {
  icon?: ReactNode
  title?: string
  description?: string
  action?: ReactNode
  compact?: boolean
}

export function EmptyState({
  icon,
  title = 'No data',
  description,
  action,
  compact = false,
}: Props) {
  return (
    <div className={`fs-empty-state${compact ? ' fs-empty-state-compact' : ''}`}>
      <div className="fs-empty-icon">{icon ?? <InboxOutlined />}</div>
      <Typography.Text strong className="fs-empty-title">{title}</Typography.Text>
      {description && (
        <Typography.Text type="secondary" className="fs-empty-desc">{description}</Typography.Text>
      )}
      {action && <div className="fs-empty-action">{action}</div>}
    </div>
  )
}
