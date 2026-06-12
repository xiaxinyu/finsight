import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Col, Progress, Row, Tag, Typography } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import type { EChartsOption } from 'echarts'
import { fetchProfile } from '../../api/analytics'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { FsChart } from '../../components/FsChart'
import { PageSkeleton } from '../../components/PageSkeleton'

const DIM_LABELS: Record<string, string> = {
  income_stability: 'Income stability',
  spending_control: 'Spending control',
  savings_discipline: 'Savings discipline',
  fixed_burden: 'Fixed burden',
  liquidity_safety: 'Liquidity safety',
  debt_pressure: 'Debt pressure',
  lifestyle_inflation: 'Lifestyle inflation',
  spending_concentration: 'Spending concentration',
  seasonality_risk: 'Seasonality risk',
  data_trust: 'Data trust',
}

export function ProfilePage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['financial-profile'],
    queryFn: fetchProfile,
  })

  if (isLoading) return <PageSkeleton />
  if (isError) {
    return (
      <DataPageLayout title="Financial Profile" icon={<UserOutlined />}>
        <Alert type="error" showIcon message="Failed to load profile" description={error instanceof Error ? error.message : ''} />
      </DataPageLayout>
    )
  }
  if (!data) return null

  const radarOption: EChartsOption = useMemo(() => ({
    tooltip: {},
    radar: {
      indicator: data.dimensions.map((d) => ({ name: DIM_LABELS[d.id] || d.id, max: 100 })),
      radius: '62%',
    },
    series: [{
      type: 'radar' as const,
      data: [{ value: data.dimensions.map((d) => d.score), name: 'Profile' }],
      areaStyle: { opacity: 0.15 },
    }],
  }), [data.dimensions])

  return (
    <DataPageLayout
      title="Financial Profile"
      subtitle={`Explainable 10-dimension view · ${data.asOf}`}
      icon={<UserOutlined />}
    >
      {data.metricsGate?.gateEnabled && !data.metricsGate.ok && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="Metrics reconciliation mismatch"
          description={`Using ${data.metricsSource === 'report_sql' ? 'report SQL fallback' : 'stored metrics'}. ${(data.metricsGate.mismatches || []).join('; ')}`}
        />
      )}
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <ContentCard title="Overall">
            <Typography.Title level={2} style={{ margin: 0 }}>{data.overallScore}</Typography.Title>
            <Tag color="blue">{data.userType.replace(/_/g, ' ')}</Tag>
            <Progress percent={data.overallScore} showInfo={false} strokeColor="#2563eb" />
          </ContentCard>
        </Col>
        <Col xs={24} md={16}>
          <ContentCard title="Dimension radar">
            <FsChart option={radarOption} height={320} />
          </ContentCard>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {data.dimensions.map((dim) => (
          <Col xs={24} md={12} lg={8} key={dim.id}>
            <ContentCard title={DIM_LABELS[dim.id] || dim.id}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <Tag>{dim.level}</Tag>
                <strong>{dim.score}</strong>
              </div>
              <Typography.Paragraph type="secondary" style={{ minHeight: 40 }}>{dim.summary}</Typography.Paragraph>
              {dim.actions?.[0]?.payload?.path && (
                <Link to={dim.actions[0].payload.path}>Drill down →</Link>
              )}
            </ContentCard>
          </Col>
        ))}
      </Row>
    </DataPageLayout>
  )
}
