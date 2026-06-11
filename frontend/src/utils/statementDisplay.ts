import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

export const BANK_LABELS: Record<string, string> = {
  CMB: 'China Merchants Bank',
  CCB: 'China Construction Bank',
  CGB: 'China Guangfa Bank',
  CRBANK: 'China Resources Bank',
  ALIPAY: 'Alipay',
  WECHAT: 'WeChat Pay',
}

export type StatementStatusKind = 'pending' | 'committed' | 'failed' | 'unknown'

export function normalizeStatementStatus(status: unknown): StatementStatusKind {
  const s = String(status || '').toLowerCase()
  if (s.includes('commit') || s === 'done' || s === 'success') return 'committed'
  if (s.includes('preview') || s.includes('pending') || s === 'uploaded') return 'pending'
  if (s.includes('fail') || s.includes('error')) return 'failed'
  return 'unknown'
}

/** Human title + distinguishing subtitle for duplicate filenames. */
export function formatStatementDisplay(
  fileName: string,
  sourceBankCode?: string | null,
  id?: string | null,
  createdAt?: unknown,
): { title: string; subtitle: string } {
  const bank = (sourceBankCode || '').toUpperCase()
  const bankLabel = BANK_LABELS[bank] || (bank || 'Statement')
  const name = (fileName || '').trim()

  const cmbApplied = name.match(/申请时间(\d{4})年(\d{2})月(\d{2})日(\d{2})时(\d{2})分/)
  const alipayRange = name.match(/(\d{4}-\d{2}-\d{2}).*?(\d{4}-\d{2}-\d{2})/)

  let title = bankLabel
  const meta: string[] = []

  if (name.includes('交易流水') || name.toLowerCase().includes('transaction')) {
    title = `${bank ? bank + ' ' : ''}Transaction Statement`
  } else if (name.includes('账单') || name.toLowerCase().includes('statement')) {
    title = `${bank ? bank + ' ' : ''}Account Statement`
  } else if (name) {
    const base = name.replace(/\.[^.]+$/, '')
    title = base.length > 48 ? `${base.slice(0, 45)}…` : base
  }

  if (cmbApplied) {
    const [, y, m, d, h, min] = cmbApplied
    meta.push(`Applied ${m}/${d}/${y} ${h}:${min}`)
  } else if (alipayRange) {
    meta.push(`${alipayRange[1]} → ${alipayRange[2]}`)
  }

  const when = formatStatementWhen(createdAt)
  if (when.relative) meta.push(`Uploaded ${when.relative}`)

  const ref = id ? String(id).slice(0, 8) : ''
  if (ref) meta.push(`Ref ${ref}`)

  return { title, subtitle: meta.join(' · ') }
}

export function formatStatementWhen(value: unknown): { date: string; relative: string } {
  if (value == null || value === '') return { date: '—', relative: '' }
  const d = dayjs(value as string | number | Date)
  if (!d.isValid()) return { date: String(value), relative: '' }
  return {
    date: d.format('MM/DD/YYYY HH:mm'),
    relative: d.fromNow(),
  }
}
