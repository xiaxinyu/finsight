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
import { ANALYTICS_STALE_MS, QUERY_KEYS } from '../../constants/queryKeys'
import { buildProfileRadarOption, PROFILE_DIM_LABELS, profileUserTypeLabel } from './profileRadar'
import { ProfileDimensionDrawer } from './ProfileDimensionDrawer'
import { profileActionLinks } from './profileActions'
import { CombinedInsightPanel } from '../../components/CombinedInsightPanel'

function dimensionIdFromRadarName(name: string): string | undefined {
  const entry = Object.entries(PROFILE_DIM_LABELS).find(([, label]) => label === name)
  return entry?.[0]
}

export function ProfilePage() {
  const { flags } = useFeatureFlags()
  const [activeDimension, setActiveDimension] = useState<ProfileDimension | null>(null)
  const { data, isLoading, isError, error } = useQuery({
    queryKey: QUERY_KEYS.financialProfile,
    queryFn: fetchProfile,
    enabled: flags.profile,
    staleTime: ANALYTICS_STALE_MS,
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

  const metricsWarning = data.metricsGate?.gateEnabled && !data.metricsGate.ok

  return (
    <DataPageLayout
      className="fs-data-page--profile"
      title="Financial Profile"
      subtitle={`Explainable 10-dimension view · ${data.asOf}`}
      icon={<UserOutlined />}
    >
      {metricsWarning && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="Metrics reconciliation mismatch"
          description={`Using stored metrics (${data.metricsSource || 'fin_metric_monthly'}). ${data.metricsGate?.warning || (data.metricsGate?.mismatches || []).join('; ')}`}
        />
      )}

      {/* Layer 1: health summary */}
      <Row gutter={[16, 16]} className="fs-profile-summary-row">
        <Col xs={24} md={8}>
          <ContentCard title="Overall">
            <Typography.Title level={2} style={{ margin: 0 }}>{data.overallScore}</Typography.Title>
            <Tag color="blue">{profileUserTypeLabel(data.userType)}</Tag>
            {data.userTypeExplanation && (
              <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 8 }}>
                {data.userTypeExplanation}
              </Typography.Paragraph>
            )}
            <Progress percent={data.overallScore} showInfo={false} strokeColor="#2563eb" />
          </ContentCard>
        </Col>
        <Col xs={24} md={16}>
          <ContentCard title="Dimension radar" className="fs-profile-radar-card">
            <div className="fs-profile-radar-wrap">
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
            </div>
            <div className="fs-profile-radar-mobile-list" aria-hidden="false">
              {data.dimensions.map((dim) => (
                <button
                  key={dim.id}
                  type="button"
                  className="fs-profile-dim-bar"
                  onClick={() => setActiveDimension(dim)}
                >
                  <span>{PROFILE_DIM_LABELS[dim.id] || dim.id}</span>
                  <span className="fs-profile-dim-bar-track">
                    <span className="fs-profile-dim-bar-fill" style={{ width: `${dim.score}%` }} />
                  </span>
                  <strong>{dim.score}</strong>
                </button>
              ))}
            </div>
          </ContentCard>
        </Col>
      </Row>

      {/* Layer 2: dimension matrix */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }} className="fs-profile-matrix-row">
        {data.dimensions.map((dim) => {
          const primaryEvidence = dim.evidence?.[0]
          const primaryAction = profileActionLinks(dim)[0]
          return (
            <Col xs={24} md={12} lg={8} key={dim.id}>
              <ContentCard
                className="fs-profile-dimension-card"
                title={PROFILE_DIM_LABELS[dim.id] || dim.id}
                extra={<Typography.Link onClick={() => setActiveDimension(dim)}>Details</Typography.Link>}
              >
                <div
                  role="button"
                  tabIndex={0}
                  className="fs-profile-dimension-body"
                  onClick={() => setActiveDimension(dim)}
                  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setActiveDimension(dim) }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <Tag>{dim.level}</Tag>
                    <strong>{dim.score}</strong>
                  </div>
                  <Typography.Paragraph
                    type="secondary"
                    className="fs-profile-dimension-reason"
                    ellipsis={{ rows: 2, tooltip: dim.reason || dim.summary }}
                  >
                    {dim.reason || dim.summary}
                  </Typography.Paragraph>
                  {primaryEvidence && (
                    <Typography.Paragraph className="fs-profile-dimension-evidence" ellipsis={{ tooltip: String(primaryEvidence.value ?? '—') }}>
                      <Typography.Text type="secondary">{primaryEvidence.label || primaryEvidence.ref}: </Typography.Text>
                      {String(primaryEvidence.value ?? '—')}
                    </Typography.Paragraph>
                  )}
                  {primaryAction && (
                    <Typography.Text type="secondary" className="fs-profile-dimension-action">
                      {primaryAction.label} →
                    </Typography.Text>
                  )}
                </div>
              </ContentCard>
            </Col>
          )
        })}
      </Row>

      {/* Layer 3: compact combined insights */}
      <div className="fs-profile-insights-row">
        <CombinedInsightPanel compact title="Actionable insights" limit={2} />
      </div>

      <ProfileDimensionDrawer
        open={!!activeDimension}
        dimension={activeDimension}
        asOf={data.asOf}
        onClose={() => setActiveDimension(null)}
      />
    </DataPageLayout>
  )
}
