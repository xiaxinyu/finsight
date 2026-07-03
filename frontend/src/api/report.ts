import { postForm } from './client'
import { normalizeResult, parseJsonArray } from './normalize'

export interface ReportPoint {
  key: string
  value: number
  code?: string
  name?: string
  level1Code?: string
  level1Name?: string
}

export async function fetchReport(endpoint: string, params: Record<string, unknown> = {}): Promise<ReportPoint[]> {
  const raw = await postForm(endpoint, params)
  const n = normalizeResult(raw)
  if (!n.ok) throw new Error(n.message || 'Request failed')
  return parseJsonArray(n.data).map((row) => {
    const p = row as ReportPoint & { key?: string; name?: string; code?: string }
    const name = String(p.name ?? p.key ?? '')
    const code = String(p.code ?? '')
    return {
      key: name || code || '',
      value: Number(p.value) || 0,
      code: code || undefined,
      name: name || undefined,
      level1Code: p.level1Code ? String(p.level1Code) : undefined,
      level1Name: p.level1Name ? String(p.level1Name) : undefined,
    }
  })
}

/** @deprecated Dashboard uses fetchMetricPeriodSummary; endpoint scheduled for removal. */
export async function homeSummary(_year: string | number, _range?: { start: string; end: string }) {
  throw new Error('homeSummary is deprecated; use fetchMetricPeriodSummary from api/analytics')
}
