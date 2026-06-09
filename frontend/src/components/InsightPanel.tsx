import { Alert } from 'antd'

export type InsightBullet = { text: string; warn?: boolean }

export function InsightPanel({ bullets }: { bullets: InsightBullet[] }) {
  if (!bullets?.length) return null
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 8 }}>
      {bullets.map((b, i) => (
        <Alert key={i} type={b.warn ? 'warning' : 'info'} showIcon message={b.text} />
      ))}
    </div>
  )
}
