import type { ProfileDimension } from '../../api/analytics'
import { PROFILE_DIM_LABELS } from './profileRadar'

export type ProfileScoreTier = 'strong' | 'fair' | 'weak'

export function profileScoreTier(score: number): ProfileScoreTier {
  if (score >= 70) return 'strong'
  if (score >= 45) return 'fair'
  return 'weak'
}

export function profileLevelTier(level?: string): ProfileScoreTier {
  const l = (level ?? '').toLowerCase()
  if (l.includes('strong') || l.includes('good') || l.includes('high')) return 'strong'
  if (l.includes('attention') || l.includes('weak') || l.includes('risk') || l.includes('low')) return 'weak'
  return 'fair'
}

export function profileScoreColor(score: number): string {
  const tier = profileScoreTier(score)
  if (tier === 'strong') return '#16a34a'
  if (tier === 'fair') return '#d97706'
  return '#dc2626'
}

export function profileDimensionLabel(id: string): string {
  return PROFILE_DIM_LABELS[id] || id
}

export function profileDimensionHighlights(dimensions: ProfileDimension[]) {
  const sorted = [...dimensions].sort((a, b) => a.score - b.score)
  return {
    weakest: sorted.slice(0, 3),
    strongest: [...dimensions].sort((a, b) => b.score - a.score).slice(0, 3),
  }
}
