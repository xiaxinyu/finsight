import { ApiError, getJson, postCommon, postForm } from './client'
import { parseJsonObject } from './normalize'
import { isCollectionResult, normalizeResult, parseJsonArray, type CollectionResult } from './normalize'

export interface TransactionRow {
  id: string
  transactionDate?: string
  bookKeepingDate?: string
  transactionDesc?: string
  balanceMoney?: number
  incomeMoney?: number
  cardTypeName?: string
  bankCode?: string
  cardTypeCode?: string
  bankCardId?: string
  bankCardName?: string
  consumeCode?: string
  consumeName?: string
  consumeID?: string
  demoArea?: string
  opponentName?: string
  txnKind?: string
  createuser?: string
  updateuser?: string
}

export interface TransactionQuery {
  page?: number
  rows?: number
  transactionDateStartStr?: string
  transactionDateEndStr?: string
  cardId?: string
  cardTypeName?: string
  consumeID?: string
  consumeName?: string
  txnTypes?: string
  demoArea?: string
  emptyConsume?: string
  opponentName?: string
  merchantToken?: string
  sortField?: 'transactionDate' | 'amount' | 'card' | 'type'
  sortOrder?: 'asc' | 'desc'
  /** v2.0.2 semantic view filter: real_income | consumption | refund | transfer | investment | liability | unclassified | data_quality */
  semanticFilter?: string
}

export async function listTransactions(params: TransactionQuery) {
  const raw = await postForm<CollectionResult<TransactionRow>>('/transaction/getTransactions', params as Record<string, unknown>)
  if (isCollectionResult<TransactionRow>(raw)) return raw
  const n = normalizeResult(raw)
  if (!n.ok) throw new ApiError(n.message || 'Failed to load transactions', 500)
  throw new ApiError('Unexpected server response — check login session', 500)
}

export type TransactionStats = {
  total: number
  income: number
  expense: number
  net: number
  transfers: number
  unclassified: number
  truncated: boolean
}

const STATS_ROW_CAP = 8000

function txnKind(row: TransactionRow): string {
  if (row.txnKind) return row.txnKind
  if (row.incomeMoney && row.incomeMoney > 0) return 'income'
  if (row.balanceMoney != null && row.balanceMoney < 0) return 'income'
  return 'expense'
}

function txnAmount(row: TransactionRow): number {
  if (row.incomeMoney && Math.abs(row.incomeMoney) > 0) return Math.abs(row.incomeMoney)
  return Math.abs(Number(row.balanceMoney || 0))
}

function isUnclassified(row: TransactionRow): boolean {
  const code = (row.consumeCode || row.consumeID || '').trim()
  const name = (row.consumeName || '').trim()
  return !code && !name
}

export function aggregateTransactionRows(rows: TransactionRow[]): Omit<TransactionStats, 'total' | 'truncated'> {
  let income = 0
  let expense = 0
  let transfers = 0
  let unclassified = 0
  for (const row of rows) {
    const kind = txnKind(row)
    if (kind === 'transfer') {
      transfers += 1
      continue
    }
    const amt = txnAmount(row)
    if (kind === 'income') income += amt
    else expense += amt
    if (isUnclassified(row)) unclassified += 1
  }
  return { income, expense, net: income - expense, transfers, unclassified }
}

export async function fetchTransactionStats(params: TransactionQuery): Promise<TransactionStats> {
  try {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v != null && v !== '') q.set(k, String(v))
    })
    const suffix = q.toString() ? `?${q.toString()}` : ''
    const raw = await getJson<Record<string, unknown>>(`/api/v1/transactions/stats${suffix}`)
    const n = normalizeResult(raw)
    if (n.ok && n.data) {
      const d = n.data as Record<string, unknown>
      return {
        total: Number(d.total || 0),
        income: Number(d.income || 0),
        expense: Number(d.expense || 0),
        net: Number(d.net || 0),
        transfers: Number(d.transfers || 0),
        unclassified: Number(d.unclassified || 0),
        truncated: Boolean(d.truncated),
      }
    }
  } catch {
    // fallback to client aggregation below
  }
  const probe = await listTransactions({ ...params, page: 1, rows: 1 })
  const total = probe.total
  if (total === 0) {
    return { total: 0, income: 0, expense: 0, net: 0, transfers: 0, unclassified: 0, truncated: false }
  }
  const rowsToLoad = Math.min(total, STATS_ROW_CAP)
  const res = await listTransactions({ ...params, page: 1, rows: rowsToLoad })
  const agg = aggregateTransactionRows(res.rows)
  return { ...agg, total, truncated: total > STATS_ROW_CAP }
}

export type { DrillBreakdownResult } from '../utils/drillBreakdown'

export async function fetchDrillBreakdown(params: TransactionQuery & { merchantToken?: string }, sampleLimit = 200) {
  const q = new URLSearchParams()
  Object.entries({ ...params, sampleLimit }).forEach(([k, v]) => {
    if (v != null && v !== '') q.set(k, String(v))
  })
  const suffix = q.toString() ? `?${q.toString()}` : ''
  const raw = await getJson<Record<string, unknown>>(`/api/v1/transactions/drill-breakdown${suffix}`)
  const n = normalizeResult<import('../utils/drillBreakdown').DrillBreakdownResult>(raw)
  if (!n.ok || !n.data) {
    throw new ApiError(n.message || 'Failed to load drill breakdown', 500)
  }
  return n.data
}

export async function updateTransaction(tx: Partial<TransactionRow>) {
  return postCommon('/transaction/update', tx as Record<string, unknown>)
}

export async function deleteTransaction(id: string) {
  return postCommon('/transaction/delete', { id })
}

export type ReclassifyPreviewRow = {
  id: string
  categoryCode?: string
  categoryName?: string
  beforeCategoryCode?: string
  beforeCategoryName?: string
  action: string
  source?: 'RULE' | 'WEAK_RULE' | 'SIMILAR' | 'HEURISTIC' | 'KEYWORDS'
  confidence?: number
  reason?: string
  suggestedKeywords?: string[]
  transactionDesc?: string
  transactionDate?: string
}

export type ReclassifyResult = {
  requested: number
  classified: number
  skipped: number
  noMatch: number
  suggested?: number
  dryRun: boolean
  preview?: ReclassifyPreviewRow[]
}

export async function classifyTransactions(ids: string, options?: { persist?: boolean; overrideExisting?: boolean }) {
  const params = new URLSearchParams()
  params.set('ids', ids)
  params.set('persist', String(options?.persist ?? true))
  if (options?.overrideExisting) {
    params.set('overrideExisting', 'true')
  }
  return postCommon(`/transaction/classify?${params.toString()}`, {})
}

/** Auto-classify all unclassified rows matching the current list filters (max 5000). */
export async function classifyUnclassifiedInFilter(filters: TransactionQuery & { persist?: boolean }) {
  const params = new URLSearchParams()
  params.set('scope', 'unclassified')
  params.set('persist', String(filters.persist ?? true))
  for (const [key, value] of Object.entries(filters)) {
    if (key === 'persist' || value == null || value === '') continue
    params.set(key, String(value))
  }
  return postCommon(`/transaction/classify?${params.toString()}`, {})
}

export function parseReclassifyResult(raw: unknown): ReclassifyResult | null {
  if (raw == null || raw === '') return null
  const direct = parseJsonObject<ReclassifyResult>(raw)
  if (direct && typeof direct.requested === 'number') return direct
  if (typeof raw === 'object' && !Array.isArray(raw)) {
    const outer = raw as { data?: unknown }
    if (outer.data != null && outer.data !== '') {
      const nested = parseJsonObject<ReclassifyResult>(outer.data)
      if (nested && typeof nested.requested === 'number') return nested
    }
  }
  return null
}

export async function incomeToExpense(ids: string) {
  return postCommon('/transaction/income-to-expense', { ids })
}

export async function expenseToIncome(ids: string) {
  return postCommon('/transaction/expense-to-income', { ids })
}

export interface KeyValue { key: string; value: string }

export async function listCards(): Promise<KeyValue[]> {
  const raw = await getJson<KeyValue[] | { data: KeyValue[] }>('/api/v1/cards/list')
  if (Array.isArray(raw)) return raw
  const n = normalizeResult<KeyValue[]>(raw)
  return parseJsonArray(n.data) as KeyValue[]
}

export async function cardTree(): Promise<TreeNode[]> {
  return getJson<TreeNode[]>('/api/v1/cards/tree')
}

export interface BankCardRow {
  id: string
  bankCode?: string
  cardTypeCode?: string
  cardNo?: string
  cardName?: string
}

export async function listBankCards(cardTypeCode?: string): Promise<BankCardRow[]> {
  const q = cardTypeCode ? `?cardTypeCode=${encodeURIComponent(cardTypeCode)}` : ''
  const raw = await getJson<BankCardRow[]>(`/api/v1/cards${q}`)
  return Array.isArray(raw) ? raw : []
}

export interface TreeNode { id: string; text: string; children?: TreeNode[] }

export async function consumeTree(txnType?: string): Promise<TreeNode[]> {
  const url = txnType ? `/api/v1/consume/tree?txnType=${encodeURIComponent(txnType)}` : '/api/v1/consume/tree'
  return getJson<TreeNode[]>(url)
}
