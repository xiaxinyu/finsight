import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Progress, Tag, Typography, message } from 'antd'
import { ReloadOutlined, UserOutlined } from '@ant-design/icons'
import { fetchProfile, fetchProfileRefresh } from '../../api/analytics'
import type { ProfileDimension } from '../../api/analytics'
import { DataPageLayout } from '../../components/DataPageLayout'
import { FsChart } from '../../components/FsChart'
import { EmptyState } from '../../components/EmptyState'
import { PageSkeleton } from '../../components/PageSkeleton'
import { useFeatureFlags } from '../../hooks/useFeatureFlags'
import { ANALYTICS_STALE_MS, QUERY_KEYS } from '../../constants/queryKeys'
import { buildProfileRadarOption, PROFILE_DIM_LABELS, profileUserTypeLabel } from './profileRadar'
import { ProfileDimensionDrawer } from './ProfileDimensionDrawer'
import { ProfileDimensionBarList } from './ProfileDimensionBarList'
import { profileActionLinks } from './profileActions'
import { CombinedInsightPanel } from '../../components/CombinedInsightPanel'
import {
  profileDimensionHighlights,
  profileDimensionLabel,
  profileDimensionVisual,
  profileLevelDisplay,
  profileScoreColor,
  profileScoreTier,
} from './profileDisplay'

function dimensionIdFromRadarName(name: string): string | undefined {
  const entry = Object.entries(PROFILE_DIM_LABELS).find(([, label]) => label === name)
  return entry?.[0]
}

function ProfileAlerts({
  needsGenerate,
  stale,
  metricsWarning,
  message: profileMessage,
  metricsGate,
  metricsSource,
}: {
  needsGenerate: boolean
  stale: boolean
  metricsWarning: boolean
  message?: string
  metricsGate?: { warning?: string; mismatches?: string[] }
  metricsSource?: string
}) {
  if (needsGenerate) {
    return (
      <Alert
        type="info"
        showIcon
        className="fs-profile-alert"
        message="Profile snapshot not ready"
        description={profileMessage || 'Click Generate profile to compute your financial profile.'}
      />
    )
  }
  if (stale) {
    return (
      <Alert
        type="warning"
        showIcon
        className="fs-profile-alert"
        message="Profile may be outdated"
        description="Your data changed since this snapshot was computed. Refresh to update scores."
      />
    )
  }
  if (metricsWarning) {
    return (
      <Alert
        type="warning"
        showIcon
        className="fs-profile-alert"
        message="Metrics reconciliation mismatch"
        description={`Using stored metrics (${metricsSource || 'fin_metric_monthly'}). ${metricsGate?.warning || (metricsGate?.mismatches || []).join('; ')}`}
      />
    )
  }
  return null
}

function DimensionCard({
  dim,
  onOpen,
}: {
  dim: ProfileDimension
  onOpen: () => void
}) {
  const visual = profileDimensionVisual(dim.id, dim.score, dim.level)
  const primaryEvidence = dim.evidence?.[0]
  const primaryAction = profileActionLinks(dim)[0]

  return (
    <button type="button" className={`fs-profile-dim-card fs-profile-dim-card--${visual.tier}`} onClick={onOpen}>
      <div className="fs-profile-dim-card__head">
        <span className="fs-profile-dim-card__title">{profileDimensionLabel(dim.id)}</span>
        <Progress
          type="circle"
          percent={dim.score}
          size={44}
          strokeColor={visual.color}
          format={(v) => <span className="fs-profile-dim-card__score">{v}</span>}
        />
      </div>
      <Tag bordered={false} className={`fs-profile-dim-card__level fs-profile-dim-card__level--${visual.tier}`}>
        {profileLevelDisplay(dim.level)}
      </Tag>
      <Typography.Paragraph className="fs-profile-dim-card__reason" ellipsis={{ rows: 2, tooltip: dim.reason || dim.summary }}>
        {dim.reason || dim.summary}
      </Typography.Paragraph>
      {primaryEvidence && (
        <Typography.Text type="secondary" className="fs-profile-dim-card__evidence" ellipsis>
          {primaryEvidence.label || primaryEvidence.ref}: {String(primaryEvidence.value ?? '—')}
        </Typography.Text>
      )}
      {primaryAction && (
        <span className="fs-profile-dim-card__action">{primaryAction.label} →</span>
      )}
    </button>
  )
}

export function ProfilePage() {
  const { flags } = useFeatureFlags()
  const queryClient = useQueryClient()
  const [activeDimension, setActiveDimension] = useState<ProfileDimension | null>(null)
  const { data, isLoading, isError, error } = useQuery({
    queryKey: QUERY_KEYS.financialProfile,
    queryFn: fetchProfile,
    enabled: flags.profile,
    staleTime: ANALYTICS_STALE_MS,
  })

  const refreshMutation = useMutation({
    mutationFn: fetchProfileRefresh,
    onSuccess: (result) => {
      if (result.busy) {
        message.info(result.message || 'Profile refresh already in progress')
        return
      }
      queryClient.setQueryData(QUERY_KEYS.financialProfile, result)
      message.success('Profile refreshed')
    },
    onError: (err) => {
      message.error(err instanceof Error ? err.message : 'Profile refresh failed')
    },
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
  const needsGenerate = data.needsRefresh || !data.materialized || data.dimensions.length === 0
  const computedLabel = data.computedAt
    ? new Date(data.computedAt).toLocaleString()
    : null
  const overallTier = profileScoreTier(data.overallScore)
  const highlights = profileDimensionHighlights(data.dimensions)

  return (
    <DataPageLayout
      className="fs-data-page--profile"
      title="Financial Profile"
      subtitle={`10-dimension health model · ${data.asOf}`}
      icon={<UserOutlined />}
      actions={(
        <Button
          type="primary"
          icon={<ReloadOutlined />}
          loading={refreshMutation.isPending}
          onClick={() => refreshMutation.mutate()}
        >
          {needsGenerate ? 'Generate profile' : 'Refresh'}
        </Button>
      )}
      extra={(
        <>
          {data.stale && <Tag color="orange">Stale</Tag>}
          {computedLabel && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              Updated {computedLabel}
            </Typography.Text>
          )}
        </>
      )}
    >
      <ProfileAlerts
        needsGenerate={needsGenerate}
        stale={!!(data.stale && !needsGenerate)}
        metricsWarning={!!metricsWarning}
        message={data.message}
        metricsGate={data.metricsGate}
        metricsSource={data.metricsSource}
      />

      <section className="fs-profile-hero">
        <div className={`fs-profile-hero__score fs-profile-hero__score--${overallTier}`}>
          <Progress
            type="dashboard"
            percent={data.overallScore}
            size={132}
            strokeWidth={10}
            strokeColor={profileScoreColor(data.overallScore)}
            format={() => (
              <div className="fs-profile-hero__score-inner">
                <span className="fs-profile-hero__score-value">{data.overallScore}</span>
                <span className="fs-profile-hero__score-label">Overall</span>
              </div>
            )}
          />
        </div>
        <div className="fs-profile-hero__meta">
          <Tag bordered={false} className={`fs-profile-hero__type fs-profile-hero__type--${overallTier}`}>
            {profileUserTypeLabel(data.userType)}
          </Tag>
          {data.confidence && (
            <span className="fs-profile-hero__confidence">
              Confidence: <strong>{data.confidence}</strong>
            </span>
          )}
          {data.sampleMonths != null && (
            <Typography.Text type="secondary" className="fs-profile-hero__sample">
              {data.sampleMonths} month(s) of data · {data.metricsSource || 'fin_metric_monthly'}
            </Typography.Text>
          )}
          {data.userTypeExplanation && (
            <Typography.Paragraph className="fs-profile-hero__explain">
              {data.userTypeExplanation}
            </Typography.Paragraph>
          )}
          <div className="fs-profile-hero__highlights">
            <div className="fs-profile-hero__highlight-col">
              <span className="fs-profile-hero__highlight-label">Needs attention</span>
              {highlights.weakest.map((d) => (
                <button key={d.id} type="button" className="fs-profile-hero__chip fs-profile-hero__chip--weak" onClick={() => setActiveDimension(d)}>
                  {profileDimensionLabel(d.id)} · {d.score}
                </button>
              ))}
            </div>
            <div className="fs-profile-hero__highlight-col">
              <span className="fs-profile-hero__highlight-label">Strengths</span>
              {highlights.strongest.map((d) => (
                <button key={d.id} type="button" className="fs-profile-hero__chip fs-profile-hero__chip--strong" onClick={() => setActiveDimension(d)}>
                  {profileDimensionLabel(d.id)} · {d.score}
                </button>
              ))}
            </div>
          </div>
        </div>
        <div className="fs-profile-hero__radar-wrap">
          <div className="fs-profile-hero__radar">
            <FsChart
              option={radarOption}
              height={280}
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
          <ProfileDimensionBarList
            dimensions={data.dimensions}
            onSelect={setActiveDimension}
          />
        </div>
      </section>

      <section className="fs-profile-dim-grid">
        {data.dimensions.map((dim) => (
          <DimensionCard key={dim.id} dim={dim} onOpen={() => setActiveDimension(dim)} />
        ))}
      </section>

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
