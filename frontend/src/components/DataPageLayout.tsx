import type { ReactNode } from 'react'
import { Typography } from 'antd'

type Props = {
  title: string
  toolbar?: ReactNode
  actions?: ReactNode
  children: ReactNode
  className?: string
}

export function DataPageLayout({ title, toolbar, actions, children, className }: Props) {
  return (
    <div className={`fs-data-page ${className ?? ''}`}>
      <div className="fs-page-topbar">
        <Typography.Title level={5} className="fs-page-title">{title}</Typography.Title>
        {actions && <div className="fs-page-actions">{actions}</div>}
      </div>
      {toolbar}
      <div className="fs-page-body">{children}</div>
    </div>
  )
}
