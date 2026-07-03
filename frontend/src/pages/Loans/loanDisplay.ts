import dayjs from 'dayjs'
import type { LoanRow, RepaymentMethod } from '../../api/loans'
import { REPAYMENT_METHOD_LABELS } from '../../api/loans'

const BANK_COLORS: Record<string, string> = {
  BOCOM: '#003087',
  ABC: '#009174',
  CZB: '#c8102e',
  DGB: '#e60012',
  CCB: '#0066b3',
  CMB: '#c41230',
  ICBC: '#c8102e',
  CRBANK: '#1a5fb4',
}

export function bankAccent(code?: string, name?: string): string {
  if (code && BANK_COLORS[code.toUpperCase()]) return BANK_COLORS[code.toUpperCase()]
  const n = name ?? ''
  if (n.includes('交通')) return BANK_COLORS.BOCOM
  if (n.includes('农业')) return BANK_COLORS.ABC
  if (n.includes('浙商')) return BANK_COLORS.CZB
  if (n.includes('东莞')) return BANK_COLORS.DGB
  if (n.includes('建设')) return BANK_COLORS.CCB
  if (n.includes('招商')) return BANK_COLORS.CMB
  return '#475569'
}

export function bankInitial(name?: string): string {
  if (!name) return '?'
  return name.replace(/银行.*$/, '').slice(0, 1) || name.slice(0, 1)
}

export function formatRate(v?: number) {
  if (v == null) return '—'
  return `${v.toFixed(2)}%`
}

export function formatDate(v?: string) {
  if (!v) return '—'
  return dayjs(v).format('YYYY-MM-DD')
}

export function repaymentLabel(v?: RepaymentMethod) {
  return v ? REPAYMENT_METHOD_LABELS[v] : null
}

export function payoffPct(row: LoanRow): number {
  const principal = row.principalAmount ?? 0
  const outstanding = row.outstandingBalance ?? principal
  if (principal <= 0) return 0
  const paid = Math.max(0, principal - outstanding)
  return Math.min(100, Math.round((paid / principal) * 100))
}

export function isActiveLoan(row: LoanRow) {
  return row.status !== 'CLOSED'
}
