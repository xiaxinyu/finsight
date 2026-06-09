import type { ReactNode } from 'react'
import { Typography } from 'antd'

type Props = {
  title: string
  subtitle?: string
  icon?: ReactNode
  toolbar?: ReactNode
  actions?: ReactNode
  extra?: ReactNode
  children: ReactNode
  className?: string
}

export function DataPageLayout({ title, subtitle, icon, toolbar, actions, extra, children, className }: Props) {
  return (
    <div className={`fs-data-page ${className ?? ''}`}>
      <div className="fs-page-topbar">
        <div className="fs-page-heading">
          {icon && <span className="fs-page-icon">{icon}</span>}
          <div>
            <Typography.Title level={5} className="fs-page-title">{title}</Typography.Title>
            {subtitle && <Typography.Text type="secondary" className="fs-page-subtitle">{subtitle}</Typography.Text>}
          </div>
        </div>
        <div className="fs-page-actions">
          {extra}
          {actions}
        </div>
      </div>
      {toolbar}
      <div className="fs-page-body">{children}</div>
    </div>
  )
}
