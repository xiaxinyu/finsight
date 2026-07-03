export type BankOption = { value: string; label: string; color?: string }

export const BANK_OPTIONS: BankOption[] = [
  { value: 'BOCOM', label: '交通银行', color: '#003087' },
  { value: 'ABC', label: '农业银行', color: '#009174' },
  { value: 'CZB', label: '浙商银行', color: '#c8102e' },
  { value: 'DGB', label: '东莞银行', color: '#e60012' },
  { value: 'CCB', label: '建设银行', color: '#0066b3' },
  { value: 'CMB', label: '招商银行', color: '#c41230' },
  { value: 'ICBC', label: '工商银行', color: '#c8102e' },
  { value: 'CRBANK', label: '华润银行', color: '#1a5fb4' },
  { value: 'BOC', label: '中国银行', color: '#c8102e' },
  { value: 'ALIPAY', label: '支付宝', color: '#1677ff' },
  { value: 'WECHAT', label: '微信支付', color: '#07c160' },
]

export const CARD_TYPE_OPTIONS = [
  { value: 'debit', label: '借记卡' },
  { value: 'credit', label: '信用卡' },
  { value: 'ewallet', label: '电子钱包' },
] as const

export type CardTypeCode = (typeof CARD_TYPE_OPTIONS)[number]['value']

export function bankLabel(code?: string): string {
  if (!code) return '—'
  return BANK_OPTIONS.find((b) => b.value === code.toUpperCase())?.label ?? code
}

export function cardTypeLabel(code?: string): string {
  if (!code) return '—'
  return CARD_TYPE_OPTIONS.find((t) => t.value === code.toLowerCase())?.label ?? code
}

export function bankAccent(code?: string): string {
  return BANK_OPTIONS.find((b) => b.value === code?.toUpperCase())?.color ?? '#475569'
}

export function bankInitial(code?: string, name?: string): string {
  const label = name || bankLabel(code)
  return label.replace(/银行.*$/, '').slice(0, 1) || label.slice(0, 1) || '?'
}

export function maskCardNo(cardNo?: string): string {
  const n = (cardNo ?? '').trim()
  if (!n) return '—'
  if (n.length <= 4) return n
  return `**** ${n.slice(-4)}`
}

export function displayCardTitle(row: { bankCode?: string; cardTypeCode?: string; cardName?: string; cardNo?: string }) {
  if (row.cardName?.trim()) return row.cardName.trim()
  const parts = [bankLabel(row.bankCode), cardTypeLabel(row.cardTypeCode)]
  const tail = maskCardNo(row.cardNo)
  if (tail !== '—') parts.push(tail)
  return parts.filter(Boolean).join(' · ')
}
