import type { AdvisorCard, ProfileEvidence } from '../api/analytics'
import { formatMoney } from './format'

export type AdvisorEvidenceItem = ProfileEvidence & {
  label?: string
  detail?: string
  value?: unknown
}

export function urgencyLabel(urgency?: string): string {
  switch (urgency) {
    case 'high': return 'High urgency'
    case 'medium': return 'Medium urgency'
    default: return 'Low urgency'
  }
}

export function urgencyColor(urgency?: string): string {
  switch (urgency) {
    case 'high': return 'red'
    case 'medium': return 'orange'
    default: return 'blue'
  }
}

export function cardPrimaryPath(card: AdvisorCard): string | undefined {
  const action = card.actions?.[0]
  if (action?.payload?.path) return action.payload.path
  return card.actionPath
}

export function cardPrimaryLabel(card: AdvisorCard): string {
  return card.actions?.[0]?.label || card.actionLabel || 'Take action'
}

export function evidenceSourceLabel(source?: string): string {
  switch (source) {
    case 'data_quality': return 'Data quality'
    case 'rule': return 'Rule evidence'
    case 'transaction_sample': return 'Transaction sample'
    case 'forecast': return 'Forecast impact'
    case 'profile': return 'Profile'
    case 'trend': return 'Trend'
    case 'merchant': return 'Merchant'
    default: return source || 'Evidence'
  }
}
export function normalizeEvidence(card: AdvisorCard): AdvisorEvidenceItem[] {
  if (card.evidence?.length) {
    return card.evidence
  }
  return (card.evidenceRefs || []).map((ref) => ({
    source: ref.source,
    ref: ref.ref,
    label: ref.ref,
    detail: `Referenced from ${ref.source}`,
  }))
}

export function formatImpact(card: AdvisorCard): string {
  const amount = Number(card.impactAmount || 0)
  if (!amount) return 'Impact not quantified'
  return formatMoney(amount)
}

export function formatConfidence(card: AdvisorCard): string {
  const confidence = Number(card.confidence || 0)
  if (!confidence) return '—'
  return `${Math.round(confidence * 100)}%`
}

export function advisorCardSummary(card: AdvisorCard): string {
  const parts = [
    card.reason || card.detail,
    Number(card.impactAmount || 0) > 0 ? `Estimated impact ${formatImpact(card)}.` : null,
    card.actions?.[0] ? `Next: ${cardPrimaryLabel(card)}.` : null,
  ].filter(Boolean)
  return parts.join(' ')
}
