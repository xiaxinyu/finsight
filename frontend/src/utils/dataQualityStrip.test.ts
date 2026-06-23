import { describe, expect, it } from 'vitest'
import { formatDataQualityStrip, type DataQualityStripData } from './dataQualityStrip'

describe('dataQualityStrip', () => {
  it('formats low confidence when unclassified pct is high', () => {
    const data: DataQualityStripData = {
      unclassifiedCount: 50,
      unclassifiedPct: 18,
      transferPairCount: 4,
      orphanCategoryTxnCount: 1,
      refundExcludedCount: 2,
      merchantTokenCoveragePct: 90,
      metricsSource: 'fin_metric_monthly',
      confidence: 'low',
    }
    const text = formatDataQualityStrip(data)
    expect(text).toContain('Low confidence')
    expect(text).toContain('18%')
    expect(text).toContain('Rule Engine')
  })
})
