import { getJson, postJson } from './client'
import { normalizeResult } from './normalize'

async function unwrap<T>(raw: unknown): Promise<T> {
  const n = normalizeResult(raw)
  if (!n.ok) throw new Error(n.message || 'Request failed')
  return n.data as T
}

export type PulseData = {
  accounts: { key: string; value: number }[]
  incomeMtd: number
  expenseMtd: number
  netFlowMtd: number
  liquidAssets: number
  dataQuality: { unclassifiedCount: number; duplicateCount: number; transferPairCount: number }
}

export async function financialPulse() {
  return unwrap<PulseData>(await getJson('/api/v1/financial-pulse'))
}

export async function cashflowMetrics() {
  return unwrap<Record<string, number>>(await getJson('/api/v1/cashflow'))
}

export async function wealthSnapshot() {
  return unwrap<Record<string, unknown>>(await getJson('/api/v1/wealth'))
}

export async function listBills() {
  return unwrap<Record<string, unknown>[]>(await getJson('/api/v1/bills'))
}

export async function saveBill(bill: Record<string, unknown>) {
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/bills', bill))
}

export async function billCalendar() {
  return unwrap<Record<string, unknown>[]>(await getJson('/api/v1/bills/calendar'))
}

export async function budgetVsActual() {
  return unwrap<Record<string, unknown>[]>(await getJson('/api/v1/budgets/vs-actual'))
}

export async function saveBudgetLine(line: Record<string, unknown>) {
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/budgets/lines', line))
}

export async function listGoals() {
  return unwrap<Record<string, unknown>[]>(await getJson('/api/v1/goals'))
}

export async function saveGoal(goal: Record<string, unknown>) {
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/goals', goal))
}

export async function simulateScenario(params: { lumpSumExpense?: number; incomeChangePct?: number; newMonthlyBill?: number }) {
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/scenarios/simulate', params))
}

export async function decisionCards() {
  return unwrap<{ type: string; text: string; actionPath: string }[]>(await getJson('/api/v1/insights/decision-cards'))
}

export async function createTransfer(fromTransactionId: string, toTransactionId: string, memo?: string) {
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/transfers', { fromTransactionId, toTransactionId, memo }))
}

export async function dataQuality() {
  return unwrap<Record<string, number>>(await getJson('/api/v1/data-quality'))
}
