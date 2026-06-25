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
  window?: string
}

export type ProfileDimension = {
  id: string
  score: number
  level: string
  summary: string
  reason?: string
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
  confidence?: 'low' | 'medium' | 'high'
  sampleMonths?: number
  userType: string
  userTypeExplanation?: string
  asOf: string
  dimensions: ProfileDimension[]
  metricsGate?: { ok?: boolean; gateEnabled?: boolean; mismatches?: string[]; warning?: string; fallbackBlocked?: boolean }
  metricsSource?: string
  materialized?: boolean
  needsRefresh?: boolean
  stale?: boolean
  computedAt?: string
  computeDurationMs?: number
  profileVersion?: string
  message?: string
  refreshed?: boolean
  busy?: boolean
}

export async function fetchProfile() {
  return unwrap<ProfileData>(await getJson('/api/v1/analytics/profile'))
}

export async function fetchProfileRefresh() {
  return unwrap<ProfileData>(await postJson('/api/v1/analytics/profile/refresh', {}))
}

export type PeriodMetricMonth = {
  yearMonth: string
  month: string
  realIncome: number
  consumptionExpense: number
  net: number
}

export type PeriodMetricSummary = {
  realIncome: number
  consumptionExpense: number
  netCashflow: number
  refundInflow?: number
  investmentOutflow?: number
  unclassifiedAmount?: number
  dataQualityScore?: number
  months: PeriodMetricMonth[]
  metricsSource: string
  periodStart?: string
  periodEnd?: string
}

export async function fetchMetricPeriodSummary(from?: string, to?: string) {
  const params = new URLSearchParams()
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return unwrap<PeriodMetricSummary>(await getJson(`/api/v1/analytics/metrics/period-summary${suffix}`))
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
  budgetTarget?: number
  deficit?: boolean
  actual?: boolean
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

export type BudgetTarget = {
  monthlyCap: number
  annualCap: number
  source: string
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
  budgetTarget?: BudgetTarget
  explanation?: string[]
  budgetSuggestion?: BudgetSuggestion
  metricsGate?: { ok?: boolean; gateEnabled?: boolean; mismatches?: string[]; warning?: string; fallbackBlocked?: boolean }
  metricsSource?: string
  adjustments?: {
    incomeChangePct?: number
    newMonthlyBill?: number
    lumpSumExpense?: number
    targetMonthlyPayment?: number
  }
  inputParams?: Record<string, unknown>
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
  combinedKind?: string
  priority?: number
  urgency?: 'high' | 'medium' | 'low' | string
  confidence?: number
  title: string
  reason?: string
  detail?: string
  impactAmount?: number
  sections?: { key: string; title: string; body: string }[]
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
  targetMonthlyPayment?: number
}): Promise<ForecastData> {
  const scenario = params.scenario
    ?? (params.incomeChangePct != null && params.incomeChangePct < -5 ? 'stress' : 'base')
  return unwrap<ForecastData>(await postJson('/api/v1/analytics/scenarios', {
    year: params.year ?? new Date().getFullYear(),
    scenario,
    incomeChangePct: params.incomeChangePct,
    lumpSumExpense: params.lumpSumExpense,
    newMonthlyBill: params.newMonthlyBill,
    targetMonthlyPayment: params.targetMonthlyPayment,
  }))
}

export async function refreshMerchantProfiles() {
  return unwrap<{ upserted: number; subscriptions: number }>(
    await postJson('/api/v1/advisor/merchants/refresh', {}),
  )
}

export async function fetchSubscriptionReport(params?: {
  transactionDateStartStr?: string
  transactionDateEndStr?: string
}) {
  const q = new URLSearchParams()
  if (params?.transactionDateStartStr) q.set('transactionDateStartStr', params.transactionDateStartStr)
  if (params?.transactionDateEndStr) q.set('transactionDateEndStr', params.transactionDateEndStr)
  const suffix = q.toString() ? `?${q.toString()}` : ''
  return unwrap<SubscriptionReport>(await getJson(`/api/v1/advisor/merchants/subscriptions${suffix}`))
}

export async function fetchMerchantConcentration() {
  return unwrap<MerchantConcentrationReport>(await getJson('/api/v1/advisor/merchants/concentration'))
}

export async function fetchMerchantDrift(year: number) {
  return unwrap<MerchantDriftReport>(await getJson(`/api/v1/advisor/merchants/drift?year=${year}`))
}
