import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import { periodToStrings } from '../components/PeriodRangePicker'
import { defaultPeriodRange, detectPresetId, formatPeriodPreview, presetRange } from './periodPresets'

describe('periodPresets', () => {
  it('formats single month preview', () => {
    const start = dayjs('2026-05-01')
    const end = dayjs('2026-05-31')
    expect(formatPeriodPreview(start, end)).toBe('May 2026')
  })

  it('defaults to all time with no API date bounds', () => {
    const range = defaultPeriodRange()
    expect(detectPresetId(range)).toBe('allTime')
    expect(periodToStrings(range)).toEqual({ start: '', end: '' })
  })

  it('detects this year preset', () => {
    const range = presetRange('thisYear')
    expect(detectPresetId(range)).toBe('thisYear')
  })

  it('last year covers full previous calendar year', () => {
    const range = presetRange('lastYear')
    const y = dayjs().subtract(1, 'year')
    expect(range[0].isSame(y.startOf('year'), 'day')).toBe(true)
    expect(range[1].isSame(y.endOf('year'), 'day')).toBe(true)
    expect(detectPresetId(range)).toBe('lastYear')
  })
})
