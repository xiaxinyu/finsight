import { describe, expect, it } from 'vitest'
import dayjs from 'dayjs'
import {
  alignedPriorYearPeriod,
  buildSpendingDriftInsights,
  buildSpendingDriftKpis,
  buildSpendingDriftRows,
  periodsComparable,
  periodDaySpan,
} from './spendingDrift'

describe('spendingDrift', () => {
  const periodA: [dayjs.Dayjs, dayjs.Dayjs] = [dayjs('2026-01-01'), dayjs('2026-06-17')]
  const fullYearB: [dayjs.Dayjs, dayjs.Dayjs] = [dayjs('2025-01-01'), dayjs('2025-12-31')]
  const alignedB = alignedPriorYearPeriod(periodA)

  it('detects uneven period lengths', () => {
    expect(periodsComparable(periodDaySpan(periodA), periodDaySpan(fullYearB))).toBe(false)
    expect(periodsComparable(periodDaySpan(periodA), periodDaySpan(alignedB))).toBe(true)
  })

  it('builds KPIs with monthly pace when spans differ', () => {
    const kpis = buildSpendingDriftKpis(
      [{ key: 'Food', value: 1000 }],
      [{ key: 'Food', value: 5000 }],
      periodA,
      fullYearB,
    )
    expect(kpis.find((k) => k.key === 'pace')?.value).toMatch(/%/)
    expect(kpis.find((k) => k.key === 'cats')?.tone).toBe('warn')
  })

  it('warns in insights when periods are not comparable', () => {
    const insights = buildSpendingDriftInsights(
      [{ key: 'Metro', value: 100 }],
      [{ key: 'Metro', value: 200 }],
      periodA,
      fullYearB,
    )
    expect(insights.some((b) => b.warn && b.text.includes('Period lengths differ'))).toBe(true)
  })

  it('sorts rows by absolute delta', () => {
    const rows = buildSpendingDriftRows(
      [{ key: 'A', value: 10 }, { key: 'B', value: 100 }],
      [{ key: 'A', value: 50 }, { key: 'B', value: 120 }],
    )
    expect(rows[0].key).toBe('A')
    expect(rows[0].delta).toBe(40)
  })
})
