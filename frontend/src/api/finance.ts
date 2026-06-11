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
  dataQuality: {
    unclassifiedCount: number
    duplicateCount: number
    duplicateExcessCount?: number
    duplicateGroupCount?: number
    transferPairCount: number
  }
}

export type DecisionCard = {
  type: string
  severity?: string
  title: string
  detail: string
  text: string
  actionPath: string
  actionLabel: string
  metric?: number
  threshold?: number
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

export async function listAccounts() {
  return unwrap<Record<string, unknown>[]>(await getJson('/api/v1/accounts'))
}

export async function accountBalances() {
  return unwrap<{ key: string; value: number }[]>(await getJson('/api/v1/accounts/balances'))
}

export async function recordSnapshot(accountId: string, balance: number, date?: number) {
  return unwrap<Record<string, unknown>>(await postJson(`/api/v1/accounts/${accountId}/snapshots`, {
    balance,
    date: date ?? Date.now(),
    source: 'manual',
  }))
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

export async function goalProgress(id: string) {
  return unwrap<Record<string, unknown>>(await getJson(`/api/v1/goals/${id}/progress`))
}

export async function simulateScenario(params: { lumpSumExpense?: number; incomeChangePct?: number; newMonthlyBill?: number }) {
  return unwrap<{
    baseline: Record<string, number>
    scenario: Record<string, number>
    delta: Record<string, number>
  }>(await postJson('/api/v1/scenarios/simulate', params))
}

export async function decisionCards() {
  return unwrap<DecisionCard[]>(await getJson('/api/v1/insights/decision-cards'))
}

export async function createTransfer(fromTransactionId: string, toTransactionId: string, memo?: string) {
  return unwrap<Record<string, unknown>>(await postJson('/api/v1/transfers', { fromTransactionId, toTransactionId, memo }))
}

export async function dataQuality() {
  return unwrap<Record<string, number>>(await getJson('/api/v1/data-quality'))
}
