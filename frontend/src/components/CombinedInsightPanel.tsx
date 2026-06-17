import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Tag } from 'antd'
import { ThunderboltOutlined } from '@ant-design/icons'
import { advisorRecommendations } from '../api/analytics'
import type { AdvisorCard } from '../api/analytics'
import { useFeatureFlags } from '../hooks/useFeatureFlags'
import { AdvisorEvidenceDrawer } from './AdvisorEvidenceDrawer'
import { AdvisorStrip } from './AdvisorStrip'
import { ContentCard } from './ContentCard'
import { combinedKindLabel, filterCombinedCards } from '../utils/combinedInsight'

type Props = {
  title?: string
  subtitle?: string
  limit?: number
}

export function CombinedInsightPanel({
  title = 'Combined insights',
  subtitle = 'Profile, trend, forecast, and merchant evidence linked together',
  limit = 3,
}: Props) {
  const { flags } = useFeatureFlags()
  const [evidenceCard, setEvidenceCard] = useState<AdvisorCard | null>(null)
  const { data: cards = [] } = useQuery({
    queryKey: ['advisor-recommendations'],
    queryFn: advisorRecommendations,
    enabled: flags.advisor,
  })

  const combined = useMemo(() => filterCombinedCards(cards).slice(0, limit), [cards, limit])
  if (!flags.advisor || !combined.length) return null

  const kinds = [...new Set(combined.map((c) => c.combinedKind || c.id).filter(Boolean))]

  return (
    <>
      <ContentCard
        title={(
          <span>
            <ThunderboltOutlined style={{ marginRight: 8 }} />
            {title}
          </span>
        )}
        extra={kinds.map((kind) => (
          <Tag key={kind} color="blue">{combinedKindLabel(kind)}</Tag>
        ))}
      >
        {subtitle && <div className="fs-combined-insight-subtitle">{subtitle}</div>}
        <AdvisorStrip cards={combined} onOpenEvidence={setEvidenceCard} />
      </ContentCard>
      <AdvisorEvidenceDrawer
        open={!!evidenceCard}
        card={evidenceCard}
        onClose={() => setEvidenceCard(null)}
      />
    </>
  )
}
