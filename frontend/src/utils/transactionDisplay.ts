import type { TransactionRow } from '../api/transaction'
import { cellText } from './cell'

const GENERIC_SEGMENT = /^(支付宝|财付通|微信|消费|支出|transfer|payment|alipay|tenpay|wechat pay|wxpay|代收付)$/i
const MASKED_TOKEN = /^[\d*@.\-]+$/
const PAYMENT_PREFIX = /^(支付宝|财付通|微信支付|银联)[-－—\s]+/

/** Mirrors Java MerchantNormalizer.merchantCoreRaw for display titles. */
export function merchantCoreRaw(raw: string | null | undefined): string {
  if (!raw?.trim()) return ''
  let s = raw.trim().replace(/^\(消费\)\s*/, '')
  if (/[\s]*[-－—][\s]*/.test(s)) {
    const parts = s.split(/[\s]*[-－—][\s]*/)
    for (let i = parts.length - 1; i >= 0; i--) {
      const part = parts[i].trim()
      if (part && !GENERIC_SEGMENT.test(part)) return part
    }
  }
  return s
}

function pickSegment(segments: string[]): string {
  let best = ''
  let bestScore = 0
  for (const partRaw of segments) {
    const part = partRaw.replace(/^@+/, '').replace(/[,，.…]+$/, '').trim()
    if (!part || part.length < 2) continue
    if (MASKED_TOKEN.test(part)) continue
    if (GENERIC_SEGMENT.test(part)) continue

    let score = Math.min(part.length, 48)
    if (/公司|集团|Inc|Ltd|LLC|Co\./i.test(part)) score += 24
    if (/^.{1,5}费$/.test(part)) score -= 8

    if (score > bestScore) {
      bestScore = score
      best = part
    }
  }
  return best
}

/** Human-readable payee / description from noisy bank export strings. */
export function cleanBankDescription(raw: string | null | undefined): string {
  if (!raw?.trim()) return ''
  let s = raw.trim().replace(PAYMENT_PREFIX, '')

  if (s.includes('@@')) {
    const picked = pickSegment(s.split('@@').map((p) => p.trim()).filter(Boolean))
    if (picked) return picked
  }
  if (s.includes('@')) {
    const picked = pickSegment(s.split('@').map((p) => p.trim()).filter(Boolean))
    if (picked) return picked
  }

  const core = merchantCoreRaw(s)
  if (core && !GENERIC_SEGMENT.test(core)) return core

  return s.replace(/\s+/g, ' ').trim()
}

export type TransactionDisplay = {
  title: string
  subtitle: string | null
  tooltip: string
}

export function buildTransactionDisplay(row: TransactionRow): TransactionDisplay {
  const opponent = cellText(row.opponentName)
  const rawDesc = cellText(row.transactionDesc)
  const memo = cellText(row.demoArea)
  const cleaned = cleanBankDescription(rawDesc)

  const title = opponent || cleaned || rawDesc || '—'
  const tooltip = [rawDesc, memo].filter(Boolean).join('\n') || title

  let subtitle: string | null = null
  if (memo && memo !== title) {
    subtitle = memo
  } else if (rawDesc && rawDesc !== title) {
    const short = rawDesc.length > 72 ? `${rawDesc.slice(0, 72)}…` : rawDesc
    if (short !== title) subtitle = short
  }

  return { title, subtitle, tooltip }
}
