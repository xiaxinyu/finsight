import { BulbOutlined, WarningOutlined } from '@ant-design/icons'

export type InsightBullet = { text: string; warn?: boolean }

export function InsightPanel({ bullets, title = 'Analysis' }: { bullets: InsightBullet[]; title?: string }) {
  if (!bullets?.length) return null
  return (
    <div className="fs-report-insights">
      <div className="fs-report-insights-title">{title}</div>
      <ul className="fs-report-insights-list">
        {bullets.map((b, i) => (
          <li key={i} className={`fs-report-insight${b.warn ? ' fs-report-insight--warn' : ''}`}>
            <span className="fs-report-insight-icon" aria-hidden>
              {b.warn ? <WarningOutlined /> : <BulbOutlined />}
            </span>
            <span>{b.text}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
