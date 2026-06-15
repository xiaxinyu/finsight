import { describe, expect, it } from 'vitest'
import type { ProfileHistoryPoint } from '../../api/analytics'
import { aggregateHistoryByDay, buildProfileHistoryOption, historyDateRange } from './profileHistoryChart'

describe('profileHistoryChart', () => {
  it('builds a six-month window ending on asOf', () => {
    const range = historyDateRange('2026-06-15', 6)
    expect(range.to).toBe('2026-06-15')
    expect(range.from).toBe('2025-12-15')
  })

  it('keeps the latest score per day', () => {
    const points: ProfileHistoryPoint[] = [
      { snapshotDate: '2026-06-01T10:00:00', dimension: 'income_stability', score: 60 },
      { snapshotDate: '2026-06-01T18:00:00', dimension: 'income_stability', score: 65 },
      { snapshotDate: '2026-06-02', dimension: 'income_stability', score: 70 },
    ]
    expect(aggregateHistoryByDay(points)).toEqual([
      { snapshotDate: '2026-06-01', dimension: 'income_stability', score: 65 },
      { snapshotDate: '2026-06-02', dimension: 'income_stability', score: 70 },
    ])
  })

  it('renders empty-state chart when no history exists', () => {
    const option = buildProfileHistoryOption('income_stability', [])
    expect(option.series).toEqual([])
    expect((option.title as { text?: string })?.text).toContain('No history')
  })

  it('maps history into a line series', () => {
    const option = buildProfileHistoryOption('income_stability', [
      { snapshotDate: '2026-05-01', dimension: 'income_stability', score: 55 },
      { snapshotDate: '2026-06-01', dimension: 'income_stability', score: 72 },
    ])
    const series = option.series as { data: number[] }[]
    expect(series[0].data).toEqual([55, 72])
  })
})
