import { describe, expect, it } from 'vitest'
import dayjs from 'dayjs'
import { buildMonthlyCashflow, mapReportByMonth, monthIndexFromKey } from './reportAnalytics'

describe('reportAnalytics', () => {
  it('maps full month names from API', () => {
    expect(monthIndexFromKey('January')).toBe(0)
    expect(monthIndexFromKey('June')).toBe(5)
    expect(monthIndexFromKey('December')).toBe(11)
  })

  it('aggregates income and expense by month key', () => {
    const inc = [{ key: 'January', value: 100 }, { key: 'March', value: 50 }]
    const exp = [{ key: 'January', value: 40 }, { key: 'February', value: 10 }]
    const period: [dayjs.Dayjs, dayjs.Dayjs] = [dayjs('2026-01-01'), dayjs('2026-03-31')]
    const rows = buildMonthlyCashflow(inc, exp, period)
    expect(rows).toHaveLength(3)
    expect(rows[0]).toMatchObject({ month: 'Jan', income: 100, expense: 40, surplus: 60 })
    expect(rows[1]).toMatchObject({ month: 'Feb', income: 0, expense: 10, surplus: -10 })
    expect(rows[2]).toMatchObject({ month: 'Mar', income: 50, expense: 0, surplus: 50 })
  })

  it('mapReportByMonth sums duplicate keys', () => {
    const m = mapReportByMonth([{ key: 'May', value: 1 }, { key: 'May', value: 2 }])
    expect(m.get(4)).toBe(3)
  })
})
