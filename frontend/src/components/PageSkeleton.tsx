import { Col, Row, Skeleton } from 'antd'

type Props = {
  variant?: 'dashboard' | 'table'
}

export function PageSkeleton({ variant = 'dashboard' }: Props) {
  if (variant === 'table') {
    return (
      <div className="fs-page-skeleton">
        <Skeleton active paragraph={{ rows: 12 }} />
      </div>
    )
  }

  return (
    <div className="fs-page-skeleton">
      <Row gutter={[12, 12]} style={{ marginBottom: 12 }}>
        {[0, 1, 2, 3].map((k) => (
          <Col key={k} xs={12} lg={6}>
            <Skeleton.Input active block style={{ height: 72, borderRadius: 8 }} />
          </Col>
        ))}
      </Row>
      <Skeleton.Input active block style={{ height: 320, borderRadius: 8 }} />
    </div>
  )
}
