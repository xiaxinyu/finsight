import { describe, expect, it } from 'vitest'
import { axisLabelInterval, axisLabelRotation, formatAxisDateLabel } from './axis'

describe('chart axis helpers', () => {
  it('formats dates as MM/DD', () => {
    expect(formatAxisDateLabel('2026-04-17')).toBe('04/17')
    expect(formatAxisDateLabel('04/17/2026')).toBe('04/17')
  })

  it('rotates when span exceeds 28', () => {
    expect(axisLabelRotation(30)).toBe(35)
    expect(axisLabelRotation(10)).toBe(0)
  })

  it('throttles axis labels for long series', () => {
    expect(axisLabelInterval(48)).toBeGreaterThanOrEqual(3)
  })
})
