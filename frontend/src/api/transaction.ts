import { ApiError, getJson, postCommon, postForm } from './client'
import { isCollectionResult, normalizeResult, parseJsonArray, type CollectionResult } from './normalize'

export interface TransactionRow {
  id: string
  transactionDate?: string
  bookKeepingDate?: string
  transactionDesc?: string
  balanceMoney?: number
  incomeMoney?: number
  cardTypeName?: string
  consumeCode?: string
  consumeName?: string
  consumeID?: string
  demoArea?: string
  txnKind?: string
  createuser?: string
  updateuser?: string
}

export interface TransactionQuery {
  page?: number
  rows?: number
  transactionDateStartStr?: string
  transactionDateEndStr?: string
  cardTypeName?: string
  consumeID?: string
  consumeName?: string
  txnTypes?: string
  demoArea?: string
  emptyConsume?: string
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

export async function updateTransaction(tx: Partial<TransactionRow>) {
  return postCommon('/transaction/update', tx as Record<string, unknown>)
}

export async function deleteTransaction(id: string) {
  return postCommon('/transaction/delete', { id })
}

export async function classifyTransactions(ids: string) {
  return postCommon('/transaction/classify', { ids })
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

export interface TreeNode { id: string; text: string; children?: TreeNode[] }

export async function consumeTree(txnType?: string): Promise<TreeNode[]> {
  const url = txnType ? `/api/v1/consume/tree?txnType=${encodeURIComponent(txnType)}` : '/api/v1/consume/tree'
  return getJson<TreeNode[]>(url)
}
