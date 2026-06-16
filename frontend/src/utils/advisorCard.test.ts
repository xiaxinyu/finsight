import { describe, expect, it } from 'vitest'
import type { AdvisorCard } from '../api/analytics'
import {
  advisorCardSummary,
  cardPrimaryPath,
  formatConfidence,
  formatImpact,
  normalizeEvidence,
  urgencyColor,
  urgencyLabel,
} from './advisorCard'

const sampleCard: AdvisorCard = {
  id: 'liquidity_safety',
  type: 'liquidity_safety',
  priority: 70,
  urgency: 'high',
  confidence: 0.82,
  title: 'Strengthen liquidity',
  reason: 'Emergency runway is below target.',
  impactAmount: 2400,
  evidence: [
    { source: 'cashflow', ref: 'runwayMonths', label: 'Emergency runway', detail: 'Months covered', value: '1.8 months' },
  ],
  actions: [{ label: 'Cash risk report', type: 'open_report', payload: { path: '/reports/cash-risk' } }],
  expiresAt: '2026-06-20T00:00:00',
}

describe('advisorCard utils', () => {
  it('labels urgency', () => {
    expect(urgencyLabel('high')).toBe('High urgency')
    expect(urgencyColor('high')).toBe('red')
  })

  it('resolves primary action path', () => {
    expect(cardPrimaryPath(sampleCard)).toBe('/reports/cash-risk')
  })

  it('formats impact and confidence', () => {
    expect(formatImpact(sampleCard)).toContain('2,400.00')
    expect(formatConfidence(sampleCard)).toBe('82%')
  })

  it('normalizes evidence from refs fallback', () => {
    const fromRefs = normalizeEvidence({
      ...sampleCard,
      evidence: undefined,
      evidenceRefs: [{ source: 'forecast', ref: '2026-03' }],
    })
    expect(fromRefs[0].ref).toBe('2026-03')
  })

  it('builds actionable summary', () => {
    const summary = advisorCardSummary(sampleCard)
    expect(summary).toContain('Emergency runway')
    expect(summary).toContain('Cash risk report')
  })
})
