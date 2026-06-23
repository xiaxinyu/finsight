import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Spin, Tag } from 'antd'
import { ThunderboltOutlined } from '@ant-design/icons'
import { advisorRecommendations } from '../api/analytics'
import type { AdvisorCard } from '../api/analytics'
import { useFeatureFlags } from '../hooks/useFeatureFlags'
import { ANALYTICS_STALE_MS, QUERY_KEYS } from '../constants/queryKeys'
import { AdvisorEvidenceDrawer } from './AdvisorEvidenceDrawer'
import { AdvisorStrip } from './AdvisorStrip'
import { ContentCard } from './ContentCard'
import { combinedKindLabel, filterCombinedCards } from '../utils/combinedInsight'

type Props = {
  title?: string
  subtitle?: string
  limit?: number
  /** Compact layout for Profile page — smaller card, fewer tags. */
  compact?: boolean
}

export function CombinedInsightPanel({
  title = 'Combined insights',
  subtitle = 'Profile, trend, forecast, and merchant evidence linked together',
  limit = 3,
  compact = false,
}: Props) {
  const { flags } = useFeatureFlags()
  const [evidenceCard, setEvidenceCard] = useState<AdvisorCard | null>(null)
  const { data: cards = [], isLoading } = useQuery({
    queryKey: QUERY_KEYS.advisorRecommendations,
    queryFn: advisorRecommendations,
    enabled: flags.advisor,
    staleTime: ANALYTICS_STALE_MS,
  })

  const combined = useMemo(() => filterCombinedCards(cards).slice(0, limit), [cards, limit])
  if (!flags.advisor) return null

  if (isLoading && !combined.length) {
    return (
      <ContentCard className={compact ? 'fs-combined-insight--compact' : undefined} title={title}>
        <Spin size="small" />
      </ContentCard>
    )
  }

  if (!combined.length) return null

  const kinds = [...new Set(combined.map((c) => c.combinedKind || c.id).filter(Boolean))]

  return (
    <>
      <ContentCard
        className={compact ? 'fs-combined-insight--compact' : undefined}
        title={(
          <span>
            <ThunderboltOutlined style={{ marginRight: 8 }} />
            {title}
          </span>
        )}
        extra={!compact && kinds.map((kind) => (
          <Tag key={kind} color="blue">{combinedKindLabel(kind)}</Tag>
        ))}
      >
        {!compact && subtitle && <div className="fs-combined-insight-subtitle">{subtitle}</div>}
        <AdvisorStrip cards={combined} onOpenEvidence={setEvidenceCard} compact={compact} />
      </ContentCard>
      <AdvisorEvidenceDrawer
        open={!!evidenceCard}
        card={evidenceCard}
        onClose={() => setEvidenceCard(null)}
      />
    </>
  )
}
