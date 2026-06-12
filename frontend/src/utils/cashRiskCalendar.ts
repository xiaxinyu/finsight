export type CashRiskEvent = {
  type: 'bill' | 'income' | 'goal' | string
  label: string
  amount: number
}

export type CashRiskDay = {
  date: string
  inflow: number
  outflow: number
  riskLevel: 'low' | 'medium' | 'high' | string
  events: CashRiskEvent[]
}

export type CashRiskMonth = {
  yearMonth: string
  net: number
  riskLevel: string
}

export type CashRiskCalendarData = {
  year: number
  scenario: string
  deficitMonths: string[]
  months: CashRiskMonth[]
  days: CashRiskDay[]
}

export function indexCashRiskDays(days: CashRiskDay[] | undefined): Map<string, CashRiskDay> {
  const map = new Map<string, CashRiskDay>()
  for (const day of days || []) {
    map.set(day.date, day)
  }
  return map
}

export function monthRiskLevel(
  months: CashRiskMonth[] | undefined,
  yearMonth: string,
): string {
  return months?.find((m) => m.yearMonth === yearMonth)?.riskLevel || 'low'
}

export function riskLevelClass(level: string): string {
  if (level === 'high') return 'fs-cash-risk-day--high'
  if (level === 'medium') return 'fs-cash-risk-day--medium'
  return 'fs-cash-risk-day--low'
}

export function eventTypeLabel(type: string): string {
  switch (type) {
    case 'bill':
      return 'Bill'
    case 'income':
      return 'Income'
    case 'goal':
      return 'Goal'
    default:
      return type
  }
}
