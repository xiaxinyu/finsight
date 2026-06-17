import { describe, expect, it } from 'vitest'
import type { AdvisorCard } from '../api/analytics'
import {
  combinedKindLabel,
  combinedSectionPreview,
  filterCombinedCards,
  isCombinedCard,
  sectionSourceLabel,
} from './combinedInsight'

const combinedCard: AdvisorCard = {
  id: 'combined_archetype_trend',
  type: 'combined',
  combinedKind: 'combined_archetype_trend',
  title: 'YoY spending drivers',
  reason: 'High fixed burden profile with rising food spend.',
  sections: [
    { key: 'profile', title: 'Profile', body: 'High fixed burden — rent-heavy' },
    { key: 'trend', title: 'Trend', body: 'Food +¥1,200 YoY' },
  ],
}

describe('combinedInsight utils', () => {
  it('detects combined cards', () => {
    expect(isCombinedCard(combinedCard)).toBe(true)
    expect(isCombinedCard({ type: 'liquidity_safety', title: 'x' })).toBe(false)
  })

  it('filters combined cards', () => {
    const cards = [
      combinedCard,
      { type: 'cashflow_risk', title: 'Risk' },
    ]
    expect(filterCombinedCards(cards)).toHaveLength(1)
    expect(filterCombinedCards(cards)[0].id).toBe('combined_archetype_trend')
  })

  it('labels kinds and section sources', () => {
    expect(combinedKindLabel('combined_forecast_pressure')).toBe('Profile + forecast')
    expect(sectionSourceLabel('merchant')).toBe('Merchants')
  })

  it('builds section preview', () => {
    const preview = combinedSectionPreview(combinedCard)
    expect(preview).toContain('Profile:')
    expect(preview).toContain('Trend:')
  })
})
