import { getJson, postJson } from './client'
import { normalizeResult } from './normalize'

async function unwrap<T>(raw: unknown): Promise<T> {
  const n = normalizeResult(raw)
  if (!n.ok) throw new Error(n.message || 'Request failed')
  return n.data as T
}

export type ProfileDimension = {
  id: string
  score: number
  level: string
  summary: string
  evidence: { source: string; ref: string; value?: unknown }[]
  actions: { label: string; type: string; payload: Record<string, string> }[]
}

export type ProfileData = {
  overallScore: number
  userType: string
  asOf: string
  dimensions: ProfileDimension[]
  metricsGate?: { ok?: boolean; gateEnabled?: boolean; mismatches?: string[] }
  metricsSource?: string
}

export async function fetchProfile() {
  return unwrap<ProfileData>(await getJson('/api/v1/analytics/profile'))
}

export async function fetchForecast(year: number, scenario = 'base') {
  return unwrap<Record<string, unknown>>(await getJson(`/api/v1/analytics/forecast?year=${year}&scenario=${scenario}`))
}

export async function fetchTrends(fromYear: number, toYear: number) {
  return unwrap<Record<string, unknown>>(await getJson(`/api/v1/analytics/trends?fromYear=${fromYear}&toYear=${toYear}`))
}

export type AdvisorCard = {
  id?: string
  type: string
  priority?: number
  title: string
  reason?: string
  detail?: string
  impactAmount?: number
  evidenceRefs?: { source: string; ref: string }[]
  actions?: { label: string; type: string; payload: Record<string, string> }[]
  actionPath?: string
  actionLabel?: string
}

export async function advisorRecommendations() {
  return unwrap<AdvisorCard[]>(await getJson('/api/v1/advisor/recommendations'))
}

export async function advisorFeedback(cardId: string, action: 'accept' | 'dismiss' | 'snooze') {
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/advisor/feedback', { cardId, action }))
}

export async function advisorAsk(question: string) {
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/advisor/ask', { question }))
}

export async function runForecastScenario(params: {
  year?: number
  scenario?: 'base' | 'conservative' | 'optimistic' | 'stress'
  incomeChangePct?: number
  lumpSumExpense?: number
  newMonthlyBill?: number
}) {
  const scenario = params.scenario
    ?? (params.incomeChangePct != null && params.incomeChangePct < -5 ? 'stress' : 'base')
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/analytics/scenarios', {
    year: params.year ?? new Date().getFullYear(),
    scenario,
    incomeChangePct: params.incomeChangePct,
    lumpSumExpense: params.lumpSumExpense,
    newMonthlyBill: params.newMonthlyBill,
  }))
}
