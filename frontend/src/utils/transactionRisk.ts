import type { TransactionRow } from '../api/transaction'
import { isFixedCostCategoryCode } from './categorySemantics'
import { cellText } from './cell'
import { rowAmount, rowTxnKind } from './transactionAmount'

export type TransactionRiskTag =
  | 'unclassified'
  | 'subscription'
  | 'fixed_cost'
  | 'transfer_candidate'
  | 'refund_candidate'
  | 'anomaly'

export type TransactionRiskTagMeta = {
  id: TransactionRiskTag
  label: string
  color: string
  hint?: string
}

const TAG_META: Record<TransactionRiskTag, TransactionRiskTagMeta> = {
  unclassified: { id: 'unclassified', label: 'Unclassified', color: 'gold', hint: 'No category assigned' },
  subscription: { id: 'subscription', label: 'Subscription', color: 'geekblue', hint: 'Likely recurring charge' },
  fixed_cost: { id: 'fixed_cost', label: 'Fixed cost', color: 'purple', hint: 'Rent, utilities, or similar' },
  transfer_candidate: { id: 'transfer_candidate', label: 'Transfer?', color: 'cyan', hint: 'May be an internal transfer' },
  refund_candidate: { id: 'refund_candidate', label: 'Refund?', color: 'blue', hint: 'Possible refund or reversal' },
  anomaly: { id: 'anomaly', label: 'Large', color: 'red', hint: 'Unusually large amount for this view' },
}

const SUBSCRIPTION_HINTS = [
  'netflix', 'spotify', 'apple.com/bill', 'adobe', 'icloud', 'youtube', 'amazon prime',
  '订阅', '会员', '月费', '自动续费',
]
const FIXED_COST_HINTS = ['rent', 'mortgage', 'insurance', '房租', '物业', '水电', '宽带', '话费']
const TRANSFER_HINTS = ['transfer', 'atm', '提现', '转账', '汇款', '内部转账']
const REFUND_HINTS = ['refund', 'reversal', 'chargeback', '退款', '退回', '冲正']

function haystack(row: TransactionRow): string {
  return [row.transactionDesc, row.demoArea, row.consumeName]
    .map((v) => cellText(v).toLowerCase())
    .filter(Boolean)
    .join(' ')
}

export function isUnclassifiedRow(row: TransactionRow): boolean {
  const code = (row.consumeCode || row.consumeID || '').trim()
  const name = (row.consumeName || '').trim()
  return !code && !name
}

function includesAny(text: string, hints: string[]): boolean {
  return hints.some((h) => text.includes(h))
}

function isFixedCostRow(row: TransactionRow): boolean {
  const code = (row.consumeCode || row.consumeID || '').trim()
  if (isFixedCostCategoryCode(code)) return true
  const text = haystack(row)
  return includesAny(text, FIXED_COST_HINTS)
}

export function detectTransactionRiskTags(
  row: TransactionRow,
  options?: { amountMax?: number; anomalyRatio?: number },
): TransactionRiskTag[] {
  const tags: TransactionRiskTag[] = []
  const text = haystack(row)
  const kind = row.txnKind || rowTxnKind(row)

  if (isUnclassifiedRow(row)) tags.push('unclassified')
  if (kind !== 'income' && includesAny(text, SUBSCRIPTION_HINTS)) tags.push('subscription')
  if (kind === 'expense' && isFixedCostRow(row)) tags.push('fixed_cost')
  if (includesAny(text, TRANSFER_HINTS)) tags.push('transfer_candidate')
  if (includesAny(text, REFUND_HINTS)) tags.push('refund_candidate')

  const amount = rowAmount(row)
  const max = options?.amountMax ?? 0
  const ratio = options?.anomalyRatio ?? 0.65
  if (max > 0 && amount >= max * ratio && amount >= 500) {
    tags.push('anomaly')
  }

  return tags
}

export function riskTagMeta(tag: TransactionRiskTag): TransactionRiskTagMeta {
  return TAG_META[tag]
}

export function amountIntensity(amount: number, maxAmount: number): number {
  if (maxAmount <= 0) return 0
  return Math.max(0, Math.min(100, (amount / maxAmount) * 100))
}
