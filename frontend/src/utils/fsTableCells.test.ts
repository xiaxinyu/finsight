import { describe, expect, it } from 'vitest'
import {
  budgetGap,
  deltaTone,
  formatDeltaPercent,
  resolveForecastKind,
} from './fsTableCells'

describe('fsTableCells', () => {
  it('classifies expense deltas as adverse when spending rises', () => {
    expect(deltaTone(1200, true)).toBe('adverse')
    expect(deltaTone(-500, true)).toBe('favorable')
    expect(deltaTone(0, true)).toBe('neutral')
  })

  it('formats delta percent with optional amount', () => {
    expect(formatDeltaPercent(12.3, 1500)).toBe('+12.3% (+¥1,500.00)')
    expect(formatDeltaPercent(-4, -200)).toBe('-4.0% (¥200.00)')
    expect(formatDeltaPercent(5)).toBe('+5.0%')
  })

  it('resolves forecast kind from row flags', () => {
    expect(resolveForecastKind({ actual: true })).toBe('actual')
    expect(resolveForecastKind({ forecast: true })).toBe('forecast')
    expect(resolveForecastKind({ budget: true })).toBe('budget')
    expect(resolveForecastKind({})).toBeNull()
  })

  it('computes budget gap as target minus expense', () => {
    expect(budgetGap(8000, 10000)).toBe(2000)
    expect(budgetGap(12000, 10000)).toBe(-2000)
    expect(budgetGap(5000, null)).toBeNull()
  })
})
