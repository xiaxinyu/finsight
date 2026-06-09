import { getJson, postForm } from './client'
import { normalizeResult, parseJsonArray } from './normalize'

export interface ReportPoint { key: string; value: number }

export async function fetchReport(endpoint: string, params: Record<string, unknown> = {}): Promise<ReportPoint[]> {
  const raw = await postForm(endpoint, params)
  const n = normalizeResult(raw)
  if (!n.ok) throw new Error(n.message || 'Request failed')
  return parseJsonArray(n.data) as ReportPoint[]
}

export async function homeSummary(year: string | number) {
  const raw = await getJson(`/transaction-report/home-summary?year=${year}`)
  const n = normalizeResult(raw)
  if (!n.ok) throw new Error(n.message || 'No summary')
  const data = typeof n.data === 'string' ? JSON.parse(n.data) : n.data
  return data as Record<string, unknown>
}
