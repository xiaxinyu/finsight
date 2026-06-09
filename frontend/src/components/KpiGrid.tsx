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
    <Row gutter={[20, 20]} className="fs-kpi-grid">
      {items.map((k) => (
        <Col key={k.key} xs={24} sm={12} lg={6}>
          <ContentCard className="fs-kpi-card" size="small">
            <Statistic title={k.label} value={k.value} valueStyle={k.color ? { color: k.color } : undefined} />
          </ContentCard>
        </Col>
      ))}
    </Row>
  )
}
