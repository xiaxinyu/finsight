import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Col, Progress, Row, Tag, Typography } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import { fetchProfile } from '../../api/analytics'
import type { ProfileDimension } from '../../api/analytics'
import { ContentCard } from '../../components/ContentCard'
import { DataPageLayout } from '../../components/DataPageLayout'
import { FsChart } from '../../components/FsChart'
import { EmptyState } from '../../components/EmptyState'
import { PageSkeleton } from '../../components/PageSkeleton'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'
import { buildProfileRadarOption, PROFILE_DIM_LABELS } from './profileRadar'

function dimensionIdFromRadarName(name: string): string | undefined {
  const entry = Object.entries(PROFILE_DIM_LABELS).find(([, label]) => label === name)
  return entry?.[0]
}
import { ProfileDimensionDrawer } from './ProfileDimensionDrawer'
import { profileActionLinks } from './profileActions'

export function ProfilePage() {
  const { flags } = useFeatureFlags()
  const [activeDimension, setActiveDimension] = useState<ProfileDimension | null>(null)
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['financial-profile'],
    queryFn: fetchProfile,
    enabled: flags.profile,
  })

  const radarOption = useMemo(
    () => buildProfileRadarOption(data?.dimensions),
    [data?.dimensions],
  )

  if (!flags.profile) {
    return (
      <DataPageLayout title="Financial Profile" icon={<UserOutlined />}>
        <EmptyState title="Profile module disabled" description="Enable finsight.profile.enabled in server configuration." />
      </DataPageLayout>
    )
  }

  if (isLoading) return <PageSkeleton />
  if (isError) {
    return (
      <DataPageLayout title="Financial Profile" icon={<UserOutlined />}>
        <Alert type="error" showIcon message="Failed to load profile" description={error instanceof Error ? error.message : ''} />
      </DataPageLayout>
    )
  }
  if (!data) return null

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
            <FsChart
              option={radarOption}
              height={320}
              onEvents={{
                click: (p) => {
                  const name = (p as { name?: string }).name
                  if (!name) return
                  const dimId = dimensionIdFromRadarName(name)
                  const dim = data.dimensions.find((d) => d.id === dimId)
                  if (dim) setActiveDimension(dim)
                },
              }}
            />
          </ContentCard>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        {data.dimensions.map((dim) => {
          const primaryEvidence = dim.evidence?.[0]
          const primaryAction = profileActionLinks(dim)[0]
          return (
            <Col xs={24} md={12} lg={8} key={dim.id}>
              <ContentCard
                title={PROFILE_DIM_LABELS[dim.id] || dim.id}
                extra={<Typography.Link onClick={() => setActiveDimension(dim)}>Details</Typography.Link>}
              >
                <div
                  role="button"
                  tabIndex={0}
                  style={{ cursor: 'pointer' }}
                  onClick={() => setActiveDimension(dim)}
                  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setActiveDimension(dim) }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <Tag>{dim.level}</Tag>
                    <strong>{dim.score}</strong>
                  </div>
                  <Typography.Paragraph type="secondary" style={{ minHeight: 40, marginBottom: 8 }}>
                    {dim.summary}
                  </Typography.Paragraph>
                  {primaryEvidence && (
                    <Typography.Paragraph style={{ marginBottom: 8, fontSize: 13 }}>
                      <Typography.Text type="secondary">{primaryEvidence.label || primaryEvidence.ref}: </Typography.Text>
                      {String(primaryEvidence.value ?? '—')}
                    </Typography.Paragraph>
                  )}
                  {primaryAction && (
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {primaryAction.label} →
                    </Typography.Text>
                  )}
                </div>
              </ContentCard>
            </Col>
          )
        })}
      </Row>

      <ProfileDimensionDrawer
        open={!!activeDimension}
        dimension={activeDimension}
        asOf={data.asOf}
        onClose={() => setActiveDimension(null)}
      />
    </DataPageLayout>
  )
}
