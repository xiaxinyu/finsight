import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { RightOutlined } from '@ant-design/icons'

export type QuickLinkItem = {
  key: string
  label: string
  to: string
  icon: ReactNode
  description?: string
}

export function QuickLinkGrid({ items }: { items: QuickLinkItem[] }) {
  return (
    <div className="fs-quick-link-grid">
      {items.map((item) => (
        <Link key={item.key} to={item.to} className="fs-quick-link-tile">
          <span className="fs-quick-link-icon">{item.icon}</span>
          <span className="fs-quick-link-text">
            <span className="fs-quick-link-label">{item.label}</span>
            {item.description && <span className="fs-quick-link-desc">{item.description}</span>}
          </span>
          <RightOutlined className="fs-quick-link-arrow" />
        </Link>
      ))}
    </div>
  )
}
