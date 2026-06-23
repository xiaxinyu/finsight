export type DataQualityStripData = {
  unclassifiedCount?: number
  unclassifiedPct?: number
  unclassifiedAmount?: number
  transferPairCount?: number
  orphanCategoryTxnCount?: number
  refundExcludedCount?: number
  merchantTokenCoveragePct?: number
  metricsSource?: string
  confidence?: 'low' | 'medium' | 'high' | string
  versions?: {
    taxonomyVersion?: number
    ruleSetVersion?: number
    metricRefreshVersion?: number
  }
}

export function formatDataQualityStrip(data: DataQualityStripData): string {
  const conf = data.confidence || 'medium'
  const confLabel = conf === 'low' ? 'Low confidence' : conf === 'high' ? 'High confidence' : 'Medium confidence'
  const pct = data.unclassifiedPct ?? 0
  const parts = [
    confLabel,
    `${data.unclassifiedCount ?? 0} unclassified (${pct}%)`,
    `${data.transferPairCount ?? 0} transfer pairs excluded`,
  ]
  if ((data.orphanCategoryTxnCount ?? 0) > 0) {
    parts.push(`${data.orphanCategoryTxnCount} orphan-category txns`)
  }
  if (data.merchantTokenCoveragePct != null) {
    parts.push(`${data.merchantTokenCoveragePct}% merchant token coverage`)
  }
  if (data.metricsSource) {
    parts.push(`metrics: ${data.metricsSource}`)
  }
  if (data.versions) {
    const v = data.versions
    parts.push(
      `taxonomy v${v.taxonomyVersion ?? '?'} · rules v${v.ruleSetVersion ?? '?'} · metrics v${v.metricRefreshVersion ?? '?'}`,
    )
  }
  if (conf === 'low') {
    parts.push('→ tune Rule Engine / classify unclassified rows before trusting trends')
  }
  return parts.join(' · ')
}
