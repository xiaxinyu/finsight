import type { TrendChangesReport } from '../utils/trendChanges'
import { getJson, postJson } from './client'
import { normalizeResult } from './normalize'
import type {
  MerchantConcentrationReport,
  MerchantDriftReport,
  SubscriptionReport,
} from '../utils/merchantReports'

async function unwrap<T>(raw: unknown): Promise<T> {
  const n = normalizeResult(raw)
  if (!n.ok) throw new Error(n.message || 'Request failed')
  return n.data as T
}

export type ProfileEvidence = {
  source: string
  ref: string
  label?: string
  detail?: string
  value?: unknown
}

export type ProfileDimension = {
  id: string
  score: number
  level: string
  summary: string
  evidence: ProfileEvidence[]
  actions: { label: string; type: string; payload: Record<string, string> }[]
}

export type ProfileHistoryPoint = {
  snapshotDate: string
  dimension: string
  score: number
  level?: string
  payload?: string
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

export async function fetchProfileHistory(from: string, to: string, dimension?: string) {
  const params = new URLSearchParams({ from, to })
  if (dimension) params.set('dimension', dimension)
  return unwrap<ProfileHistoryPoint[]>(await getJson(`/api/v1/analytics/profile/history?${params}`))
}

export type ForecastMonth = {
  yearMonth: string
  income: number
  expense: number
  net: number
  incomeLower?: number
  incomeUpper?: number
  expenseLower?: number
  expenseUpper?: number
  netLower?: number
  netUpper?: number
  deficit?: boolean
  forecast?: boolean
}

export type BudgetSuggestion = {
  monthlyCap: number
  annualCap: number
  note: string
}

export type ForecastCategoryMonth = {
  yearMonth: string
  amount: number
  amountLower?: number
  amountUpper?: number
}

export type ForecastCategory = {
  categoryCode: string
  categoryName: string
  yearTotal: number
  yearTotalLower?: number
  yearTotalUpper?: number
  sharePct: number
  months: ForecastCategoryMonth[]
}

export type ForecastConfidence = {
  halfWidthPct: number
  method: string
}

export type ForecastData = {
  year: number
  scenario: string
  runId: string
  yearIncome: number
  yearExpense: number
  yearNet: number
  yearIncomeLower?: number
  yearIncomeUpper?: number
  yearExpenseLower?: number
  yearExpenseUpper?: number
  yearNetLower?: number
  yearNetUpper?: number
  deficitMonths: string[]
  months: ForecastMonth[]
  categoryForecasts?: ForecastCategory[]
  confidence?: ForecastConfidence
  budgetSuggestion?: BudgetSuggestion
  metricsGate?: { ok?: boolean; gateEnabled?: boolean; mismatches?: string[] }
  metricsSource?: string
}

export type ForecastCategoryResponse = {
  year: number
  scenario: string
  runId: string
  confidence?: ForecastConfidence
  categories: ForecastCategory[]
}

export async function fetchForecast(year: number, scenario = 'base') {
  return unwrap<ForecastData>(await getJson(`/api/v1/analytics/forecast?year=${year}&scenario=${scenario}`))
}

export async function fetchForecastCategories(year: number, scenario = 'base') {
  return unwrap<ForecastCategoryResponse>(
    await getJson(`/api/v1/analytics/forecast/categories?year=${year}&scenario=${scenario}`),
  )
}

export type CashRiskCalendarResponse = {
  year: number
  scenario: string
  deficitMonths: string[]
  months: { yearMonth: string; net: number; riskLevel: string }[]
  days: {
    date: string
    inflow: number
    outflow: number
    riskLevel: string
    events: { type: string; label: string; amount: number }[]
  }[]
}

export async function fetchCashRiskCalendar(year: number, scenario = 'stress') {
  return unwrap<CashRiskCalendarResponse>(
    await getJson(`/api/v1/analytics/cash-risk-calendar?year=${year}&scenario=${scenario}`),
  )
}

export async function fetchTrends(fromYear: number, toYear: number) {
  return unwrap<TrendChangesReport>(
    await getJson(`/api/v1/analytics/trends?fromYear=${fromYear}&toYear=${toYear}`),
  )
}

export type AdvisorCard = {
  id?: string
  type: string
  priority?: number
  urgency?: 'high' | 'medium' | 'low' | string
  confidence?: number
  title: string
  reason?: string
  detail?: string
  impactAmount?: number
  evidence?: ProfileEvidence[]
  evidenceRefs?: { source: string; ref: string }[]
  actions?: { label: string; type: string; payload: Record<string, string> }[]
  expiresAt?: string
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

export async function refreshMerchantProfiles() {
  return unwrap<{ upserted: number; subscriptions: number }>(
    await postJson('/api/v1/advisor/merchants/refresh', {}),
  )
}

export async function fetchSubscriptionReport() {
  return unwrap<SubscriptionReport>(await getJson('/api/v1/advisor/merchants/subscriptions'))
}

export async function fetchMerchantConcentration() {
  return unwrap<MerchantConcentrationReport>(await getJson('/api/v1/advisor/merchants/concentration'))
}

export async function fetchMerchantDrift(year: number) {
  return unwrap<MerchantDriftReport>(await getJson(`/api/v1/advisor/merchants/drift?year=${year}`))
}
