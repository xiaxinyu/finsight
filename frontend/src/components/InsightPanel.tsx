import { Alert } from 'antd'
import { BulbOutlined, WarningOutlined } from '@ant-design/icons'

export type InsightBullet = { text: string; warn?: boolean }

export function InsightPanel({ bullets }: { bullets: InsightBullet[] }) {
  if (!bullets?.length) return null
  return (
    <div className="fs-insight-panel">
      {bullets.map((b, i) => (
        <Alert
          key={i}
          type={b.warn ? 'warning' : 'info'}
          showIcon
          icon={b.warn ? <WarningOutlined /> : <BulbOutlined />}
          message={b.text}
        />
      ))}
    </div>
  )
}
