import { describe, expect, it } from 'vitest'
import {
  indexCashRiskDays,
  monthRiskLevel,
  riskLevelClass,
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
})
