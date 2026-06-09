import { postForm } from './client'
import { isCollectionResult, type CollectionResult } from './normalize'
import type { TransactionRow } from './transaction'

export async function listLedger(endpoint: string, params: Record<string, unknown> = {}) {
  const raw = await postForm<CollectionResult<TransactionRow>>(endpoint, params)
  if (isCollectionResult<TransactionRow>(raw)) return raw
  return { total: 0, rows: [] as TransactionRow[] }
}
