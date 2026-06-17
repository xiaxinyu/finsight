import type { AdvisorCard } from '../api/analytics'

export type CombinedSection = { key: string; title: string; body: string }

export function isCombinedCard(card: AdvisorCard): boolean {
  return card.type === 'combined' || !!card.combinedKind
}

export function filterCombinedCards(cards: AdvisorCard[]): AdvisorCard[] {
  return cards.filter(isCombinedCard)
}

export function combinedKindLabel(kind?: string): string {
  switch (kind) {
    case 'combined_archetype_trend':
      return 'Profile + trend'
    case 'combined_forecast_pressure':
      return 'Profile + forecast'
    case 'combined_subscription_review':
      return 'Profile + merchants'
    case 'combined_data_quality':
      return 'Data quality'
    default:
      return 'Combined insight'
  }
}

export function sectionSourceLabel(key: string): string {
  switch (key) {
    case 'profile':
      return 'Profile'
    case 'trend':
      return 'Trend'
    case 'forecast':
      return 'Forecast'
    case 'merchant':
      return 'Merchants'
    case 'evidence':
      return 'Evidence'
    default:
      return key
  }
}

export function combinedSectionPreview(card: AdvisorCard): string {
  const sections = card.sections || []
  if (!sections.length) return card.reason || card.detail || ''
  return sections.map((s) => `${s.title}: ${s.body}`).join(' · ')
}
