import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import { detectPresetId, formatPeriodPreview, presetRange } from './periodPresets'

describe('periodPresets', () => {
  it('formats single month preview', () => {
    const start = dayjs('2026-05-01')
    const end = dayjs('2026-05-31')
    expect(formatPeriodPreview(start, end)).toBe('May 2026')
  })

  it('detects this year preset', () => {
    const range = presetRange('thisYear')
    expect(detectPresetId(range)).toBe('thisYear')
  })
})
