import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import {
  calendarSelectedKey,
  indexCashRiskDays,
  monthRiskLevel,
  riskLevelClass,
  syncSelectedDayToYear,
  type CashRiskDay,
} from './cashRiskCalendar'

const sampleDays: CashRiskDay[] = [
  {
    date: '2026-03-10',
    inflow: 0,
    outflow: 3000,
    riskLevel: 'high',
    events: [{ type: 'bill', label: 'Rent', amount: 3000 }],
  },
  {
    date: '2026-03-05',
    inflow: 5000,
    outflow: 0,
    riskLevel: 'medium',
    events: [{ type: 'income', label: 'Estimated income', amount: 5000 }],
  },
]

describe('cashRiskCalendar utils', () => {
  it('indexes days by ISO date', () => {
    const map = indexCashRiskDays(sampleDays)
    expect(map.get('2026-03-10')?.riskLevel).toBe('high')
    expect(map.get('2026-03-05')?.events[0].type).toBe('income')
  })

  it('resolves month risk level', () => {
    expect(monthRiskLevel([
      { yearMonth: '2026-03', net: -100, riskLevel: 'high' },
    ], '2026-03')).toBe('high')
    expect(monthRiskLevel([], '2026-04')).toBe('low')
  })

  it('maps risk level to css class', () => {
    expect(riskLevelClass('high')).toBe('fs-cash-risk-day--high')
    expect(riskLevelClass('low')).toBe('fs-cash-risk-day--low')
  })

  it('syncSelectedDayToYear moves month/day into target year', () => {
    const selected = dayjs('2025-06-15')
    expect(syncSelectedDayToYear(selected, 2026).format('YYYY-MM-DD')).toBe('2026-06-15')
    expect(syncSelectedDayToYear(selected, 2027).format('YYYY-MM-DD')).toBe('2027-06-15')
  })

  it('syncSelectedDayToYear clamps Feb 29 to last day in non-leap years', () => {
    const leapDay = dayjs('2024-02-29')
    expect(syncSelectedDayToYear(leapDay, 2025).format('YYYY-MM-DD')).toBe('2025-02-28')
  })

  it('calendarSelectedKey matches calendar panel year', () => {
    expect(calendarSelectedKey(dayjs('2025-03-10'), 2026)).toBe('2026-03-10')
    expect(indexCashRiskDays(sampleDays).get(
      calendarSelectedKey(dayjs('2025-03-10'), 2026),
    )?.riskLevel).toBe('high')
  })
})
