import { describe, expect, it } from 'vitest'
import dayjs from 'dayjs'
import { isFilterDirty } from './useFilterApply'

describe('isFilterDirty', () => {
  it('returns false when draft matches applied', () => {
    expect(isFilterDirty({ start: '01/01/2026', end: '06/01/2026' }, { start: '01/01/2026', end: '06/01/2026' })).toBe(false)
  })

  it('returns true when a field differs', () => {
    expect(isFilterDirty({ start: '01/01/2026', end: '06/01/2026' }, { start: '02/01/2026', end: '06/01/2026' })).toBe(true)
  })

  it('compares dayjs date ranges by formatted value', () => {
    const a = { dateRange: [dayjs('2026-01-01'), dayjs('2026-06-01')] }
    const b = { dateRange: [dayjs('2026-01-01'), dayjs('2026-06-01')] }
    const c = { dateRange: [dayjs('2026-01-01'), dayjs('2026-07-01')] }
    expect(isFilterDirty(a, b)).toBe(false)
    expect(isFilterDirty(a, c)).toBe(true)
  })
})
