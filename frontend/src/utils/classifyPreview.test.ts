import { describe, expect, it } from 'vitest'
import { buildClassifyPreviewRows, isBulkManualMode } from './classifyPreview'
import type { ReclassifyPreviewRow } from '../api/transaction'
import type { TransactionRow } from '../api/transaction'

const selected: TransactionRow[] = [
  { id: 'a', transactionDesc: 'Pay A', consumeName: 'Food', consumeCode: 'FOOD' },
  { id: 'b', transactionDesc: 'Pay B' },
]

describe('buildClassifyPreviewRows', () => {
  it('includes selected rows when preview is empty', () => {
    const rows = buildClassifyPreviewRows([], selected)
    expect(rows).toHaveLength(2)
    expect(rows.every((r) => r.action === 'MANUAL')).toBe(true)
  })

  it('merges preview with missing selected rows', () => {
    const preview: ReclassifyPreviewRow[] = [{
      id: 'a',
      action: 'PREVIEW',
      categoryCode: 'TRAVEL',
      categoryName: 'Travel',
    }]
    const rows = buildClassifyPreviewRows(preview, selected)
    expect(rows).toHaveLength(2)
    expect(rows.find((r) => r.id === 'b')?.action).toBe('MANUAL')
  })
})

describe('isBulkManualMode', () => {
  it('is true when no auto matches and all manual', () => {
    expect(isBulkManualMode([
      { id: '1', action: 'MANUAL' },
      { id: '2', action: 'MANUAL' },
    ], { classified: 0 })).toBe(true)
    expect(isBulkManualMode([
      { id: '1', action: 'PREVIEW', categoryCode: 'FOOD' },
    ], { classified: 1 })).toBe(false)
  })
})
