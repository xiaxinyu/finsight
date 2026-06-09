import { Col, Row, Statistic } from 'antd'
import { ContentCard } from './ContentCard'

export type KpiItem = {
  key: string
  label: string
  value: string
  color?: string
}

export function KpiGrid({ items }: { items: KpiItem[] }) {
  return (
    <Row gutter={[12, 12]} className="fs-kpi-grid">
      {items.map((k) => (
        <Col key={k.key} xs={12} sm={12} lg={6}>
          <ContentCard className="fs-kpi-card" size="small" styles={{ body: { padding: '10px 12px' } }}>
            <Statistic title={k.label} value={k.value} valueStyle={k.color ? { color: k.color, fontSize: 18 } : { fontSize: 18 }} />
          </ContentCard>
        </Col>
      ))}
    </Row>
  )
}
