import { describe, expect, it } from 'vitest'
import { formatMoney, formatNumber } from './format'

describe('formatMoney', () => {
  it('formats with symbol by default', () => {
    expect(formatMoney(1234.5)).toBe('¥1,234.50')
  })

  it('formats without symbol when requested', () => {
    expect(formatMoney(1234.5, { symbol: false })).toBe('1,234.50')
  })
})

describe('formatNumber', () => {
  it('returns empty for NaN', () => {
    expect(formatNumber(NaN)).toBe('')
  })
})
