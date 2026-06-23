import { getJson, postJson } from './client'
import { normalizeResult } from './normalize'
import type { ReclassifyResult, TransactionQuery } from './transaction'
import { parseReclassifyResult } from './transaction'

function buildFilterParams(filters: Record<string, unknown>): URLSearchParams {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value == null || value === '') continue
    params.set(key, String(value))
  }
  return params
}

export async function previewReclassificationByIds(
  ids: string,
  overrideExisting = false,
): Promise<ReclassifyResult | null> {
  const params = new URLSearchParams()
  params.set('ids', ids)
  params.set('overrideExisting', String(overrideExisting))
  const raw = await postJson(`/api/v1/classification/reclassification/preview?${params.toString()}`, {})
  return parseReclassifyResult(raw)
}

export async function previewReclassificationUnclassified(
  filters: TransactionQuery,
): Promise<ReclassifyResult | null> {
  const params = buildFilterParams(filters as Record<string, unknown>)
  const raw = await postJson(`/api/v1/classification/reclassification/preview?${params.toString()}`, {})
  return parseReclassifyResult(raw)
}

export type ReclassificationApplyResult = {
  batchId?: string
  result?: ReclassifyResult
  dirtyMonths?: Array<{ monthKey?: string }>
}

export async function applyReclassification(
  ids: string,
  overrideExisting = false,
  reason?: string,
): Promise<ReclassificationApplyResult> {
  const params = new URLSearchParams()
  params.set('ids', ids)
  params.set('overrideExisting', String(overrideExisting))
  if (reason) params.set('reason', reason)
  return postJson(`/api/v1/classification/reclassification/apply?${params.toString()}`, {}) as Promise<ReclassificationApplyResult>
}

export async function fetchReclassificationBatches(limit = 20) {
  return getJson(`/api/v1/classification/reclassification/batches?limit=${limit}`) as Promise<Array<Record<string, unknown>>>
}

export async function fetchForecastBacktest(months = 6) {
  const res = normalizeResult(await getJson(`/api/v1/maintenance/forecast-backtest?months=${months}`))
  return res.data as {
    incomeMape?: number | null
    expenseMape?: number | null
    sampleMonths?: number
    months?: Array<{ monthKey?: string; incomeErrorPct?: number; expenseErrorPct?: number }>
  }
}
