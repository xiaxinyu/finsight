import { describe, expect, it } from 'vitest'
import { formatPartialDrillMessage, isDrillTruncated, mapBreakdownRows } from './drillBreakdown'

describe('drillBreakdown', () => {
  it('formats partial drill warning when total exceeds sample', () => {
    const msg = formatPartialDrillMessage(1500, 200)
    expect(msg).toContain('200')
    expect(msg).toContain('1,500')
    expect(msg).toContain('sample')
  })

  it('returns empty message when sample covers total', () => {
    expect(formatPartialDrillMessage(50, 200)).toBe('')
  })

  it('detects truncated drill results', () => {
    expect(isDrillTruncated({ total: 500, sampleSize: 200, truncated: true })).toBe(true)
    expect(isDrillTruncated({ total: 50, sampleSize: 50, truncated: false })).toBe(false)
  })

  it('maps backend breakdown rows for tables', () => {
    const rows = mapBreakdownRows([
      { code: 'FOOD', label: 'Food', txnCount: 3, total: 120 },
    ], 'category')
    expect(rows[0]).toMatchObject({ key: 'FOOD', label: 'Food', count: 3, total: 120 })
  })
})
