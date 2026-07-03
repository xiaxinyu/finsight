import { Link } from 'react-router-dom'
import { Alert, Button, Col, Row, Statistic } from 'antd'
import { LinkOutlined, TagsOutlined, WarningOutlined } from '@ant-design/icons'

type Props = {
  imported: number
  skippedDuplicates?: number
  unclassifiedCount: number
  possibleDuplicateCount?: number
  cardId?: string
  cardLabel?: string
}

export function ImportQualityGate({
  imported, skippedDuplicates = 0, unclassifiedCount, possibleDuplicateCount = 0, cardId, cardLabel,
}: Props) {
  const txLink = cardId ? `/transactions?cardId=${encodeURIComponent(cardId)}` : '/transactions'
  const unclassifiedLink = `${txLink}${cardId ? '&' : '?'}emptyConsume=1`

  return (
    <div className="fs-import-quality-gate">
      <Alert
        type="success"
        showIcon
        message="Import complete — review data quality before trusting reports"
        description="Classified transactions feed Dashboard KPIs and all semantic reports. Fix gaps below if numbers look off."
        className="fs-import-quality-gate-banner"
      />
      <Row gutter={[12, 12]} className="fs-import-quality-gate-stats">
        <Col xs={12} sm={6}>
          <Statistic title="Imported" value={imported} valueStyle={{ fontSize: 22 }} />
        </Col>
        <Col xs={12} sm={6}>
          <Statistic
            title="Unclassified"
            value={unclassifiedCount}
            valueStyle={{ fontSize: 22, color: unclassifiedCount > 0 ? '#ea580c' : undefined }}
          />
        </Col>
        <Col xs={12} sm={6}>
          <Statistic title="Skipped dupes" value={skippedDuplicates} valueStyle={{ fontSize: 22 }} />
        </Col>
        <Col xs={12} sm={6}>
          <Statistic
            title="Flagged in preview"
            value={possibleDuplicateCount}
            valueStyle={{ fontSize: 22, color: possibleDuplicateCount > 0 ? '#ca8a04' : undefined }}
          />
        </Col>
      </Row>
      {cardLabel && (
        <div className="fs-import-quality-gate-card">
          Bound to card: <strong>{cardLabel}</strong>
        </div>
      )}
      <div className="fs-import-quality-gate-actions">
        <Link to={txLink}>
          <Button type="primary" icon={<LinkOutlined />}>View transactions</Button>
        </Link>
        {unclassifiedCount > 0 && (
          <Link to={unclassifiedLink}>
            <Button icon={<TagsOutlined />}>Review unclassified ({unclassifiedCount})</Button>
          </Link>
        )}
        {unclassifiedCount > imported * 0.2 && imported > 0 && (
          <Alert
            type="warning"
            showIcon
            icon={<WarningOutlined />}
            message="High unclassified ratio"
            description="Over 20% of imported rows have no category — trends and budget reports may under-count spending."
          />
        )}
      </div>
    </div>
  )
}
