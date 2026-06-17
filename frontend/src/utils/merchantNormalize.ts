/** Mirrors Java MerchantNormalizer for stable drill-down token matching. */

const ORDER_NO = /(?:订单|order\s*no\.?\s*:?|ord(?:er)?\s*[#:])\s*\d{4,}/gi
const STORE_NO = /(?:门店|store|branch|shop)\s*[#:]?\s*\d{2,}/gi
const TRAILING_DIGITS = /\s+\d{4,}$/
const PAYMENT_CHANNEL = /(?:alipay|wechat\s*pay|wxpay|tenpay|unionpay|银联|支付宝|微信支付|财付通)/gi
const DOMAIN_SUFFIX = /\.(com|cn|net|io)$/
const TRAILING_NOISE = /\s+(trip|trips|ride|rides|monthly|annual|subscription|mktp)$/i
const MULTI_SPACE = /\s+/g

export function rawMerchant(opponentName?: string | null, transactionDesc?: string | null): string {
  const opponent = (opponentName || '').trim()
  if (opponent) return opponent
  return (transactionDesc || '').trim()
}

export function normalizeMerchantToken(raw: string | null | undefined): string {
  if (!raw?.trim()) return ''
  let normalized = raw.toLowerCase().trim()
  normalized = normalized.replace(ORDER_NO, '')
  normalized = normalized.replace(STORE_NO, '')
  normalized = normalized.replace(PAYMENT_CHANNEL, '')
  normalized = normalized.replace(TRAILING_DIGITS, '')
  normalized = normalized.replace(TRAILING_NOISE, '')
  normalized = normalized.replace(DOMAIN_SUFFIX, '')
  normalized = normalized.replace(MULTI_SPACE, ' ').trim()
  return normalized
}

export function rowMatchesMerchantToken(
  opponentName: string | undefined,
  transactionDesc: string | undefined,
  merchantToken: string,
): boolean {
  const raw = rawMerchant(opponentName, transactionDesc)
  return normalizeMerchantToken(raw) === merchantToken
}
