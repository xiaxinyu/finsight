import type { ProfileDimension } from '../../api/analytics'
import { PROFILE_DIM_LABELS } from './profileRadar'

export type ProfileScoreTier = 'strong' | 'fair' | 'weak'

const DEFAULT_THRESHOLDS = { strong: 70, fair: 45 }

/** Stricter bands for risk / pressure dimensions — red/orange appear sooner. */
const DIMENSION_THRESHOLDS: Record<string, { strong: number; fair: number }> = {
  debt_pressure: { strong: 80, fair: 58 },
  fixed_burden: { strong: 78, fair: 55 },
  income_stability: { strong: 72, fair: 48 },
  seasonality_risk: { strong: 72, fair: 52 },
  lifestyle_inflation: { strong: 72, fair: 52 },
  spending_concentration: { strong: 72, fair: 50 },
}

const TIER_COLORS: Record<ProfileScoreTier, string> = {
  strong: '#16a34a',
  fair: '#d97706',
  weak: '#dc2626',
}

const TIER_ANT_TAG: Record<ProfileScoreTier, 'success' | 'warning' | 'error'> = {
  strong: 'success',
  fair: 'warning',
  weak: 'error',
}

export type ProfileDimensionVisual = {
  tier: ProfileScoreTier
  color: string
  antTagColor: 'success' | 'warning' | 'error'
}

export function profileDimensionThresholds(dimensionId?: string) {
  if (dimensionId && DIMENSION_THRESHOLDS[dimensionId]) {
    return DIMENSION_THRESHOLDS[dimensionId]
  }
  return DEFAULT_THRESHOLDS
}

export function profileDimensionScoreTier(dimensionId: string, score: number): ProfileScoreTier {
  const { strong, fair } = profileDimensionThresholds(dimensionId)
  if (score >= strong) return 'strong'
  if (score >= fair) return 'fair'
  return 'weak'
}

/** Overall / non-dimension-specific tier (hero score, etc.). */
export function profileScoreTier(score: number): ProfileScoreTier {
  if (score >= DEFAULT_THRESHOLDS.strong) return 'strong'
  if (score >= DEFAULT_THRESHOLDS.fair) return 'fair'
  return 'weak'
}

/** Fallback when only backend level string is available — exact match, no substring traps. */
export function profileLevelTier(level?: string): ProfileScoreTier {
  const l = (level ?? '').toLowerCase().trim().replace(/\s+/g, '_')
  if (l === 'strong' || l === 'good') return 'strong'
  if (l === 'needs_attention' || l === 'weak' || l === 'critical') return 'weak'
  if (l === 'moderate' || l === 'fair' || l === 'medium') return 'fair'
  return 'fair'
}

export function profileDimensionVisual(dimensionId: string, score: number, level?: string): ProfileDimensionVisual {
  const tier = Number.isFinite(score)
    ? profileDimensionScoreTier(dimensionId, score)
    : profileLevelTier(level)
  return {
    tier,
    color: TIER_COLORS[tier],
    antTagColor: TIER_ANT_TAG[tier],
  }
}

export function profileScoreColor(score: number, dimensionId?: string): string {
  const tier = dimensionId
    ? profileDimensionScoreTier(dimensionId, score)
    : profileScoreTier(score)
  return TIER_COLORS[tier]
}

export function profileLevelDisplay(level?: string): string {
  return (level ?? '').replace(/_/g, ' ')
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

/** Dimensions sorted for the side bar: lowest scores (needs attention) first. */
export function profileDimensionsByPriority(dimensions: ProfileDimension[]): ProfileDimension[] {
  return [...dimensions].sort((a, b) => a.score - b.score)
}
