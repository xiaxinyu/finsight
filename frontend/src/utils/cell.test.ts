import { describe, expect, it } from 'vitest'
import { cellText, formatTableDate } from './cell'

describe('cellText', () => {
  it('returns string as-is', () => {
    expect(cellText('招商银行')).toBe('招商银行')
  })

  it('returns empty for object', () => {
    expect(cellText({ foo: 1 })).toBe('')
  })

  it('returns empty for null', () => {
    expect(cellText(null)).toBe('')
  })
})

describe('formatTableDate', () => {
  it('formats ISO string', () => {
    expect(formatTableDate('2026-03-20T16:00:00.000+00:00')).toBe('03/20/2026')
  })

  it('returns empty for invalid', () => {
    expect(formatTableDate('')).toBe('')
  })
})
