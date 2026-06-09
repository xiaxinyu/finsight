import { getJson, postCommon, postForm } from './client'
import { isCollectionResult, normalizeResult, parseJsonArray, type CollectionResult } from './normalize'

export interface TransactionRow {
  id: string
  transactionDate?: string
  bookKeepingDate?: string
  transactionDesc?: string
  balanceMoney?: number
  cardTypeName?: string
  consumeCode?: string
  consumeName?: string
  consumeID?: string
  demoArea?: string
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
  txnTypes?: string
  demoArea?: string
}

export async function listTransactions(params: TransactionQuery) {
  const raw = await postForm<CollectionResult<TransactionRow>>('/transaction/getTransactions', params as Record<string, unknown>)
  if (isCollectionResult<TransactionRow>(raw)) return raw
  return { total: 0, rows: [] as TransactionRow[] }
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
