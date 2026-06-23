import { deleteReq, getJson, postJson, putJson } from './client'
import { isCollectionResult } from './normalize'

export type ConsumeRuleRow = {
  id?: string
  categoryId?: string
  pattern?: string
  patternType?: string
  priority?: number
  active?: number
  bankCode?: string
  cardTypeCode?: string
  remark?: string
  tags?: string[]
  minAmount?: number
  maxAmount?: number
}

export type ConsumeCategoryRow = {
  id?: string
  parentId?: string
  code?: string
  name?: string
  level?: number
  sortNo?: number
  deleted?: number
  txnTypes?: string
}

export async function listUsers() {
  return getJson('/api/v1/users')
}

export async function createUser(user: Record<string, unknown>) {
  return postJson('/api/v1/users', user)
}

export async function updateUser(id: number | string, user: Record<string, unknown>) {
  return putJson(`/api/v1/users/${id}`, user)
}

export async function deleteUser(id: number | string) {
  return deleteReq(`/api/v1/users/${id}`)
}

export async function listCardsAdmin() {
  return getJson('/api/v1/cards')
}

export async function createCard(card: Record<string, unknown>) {
  return postJson('/api/v1/cards', card)
}

export async function updateCard(id: string, card: Record<string, unknown>) {
  return putJson(`/api/v1/cards/${id}`, card)
}

export async function deleteCard(id: string) {
  return deleteReq(`/api/v1/cards/${id}`)
}

export async function listRules(includeInactive = true, includeInvalid = false) {
  const params = new URLSearchParams()
  if (includeInactive) params.set('includeInactive', 'true')
  if (includeInvalid) params.set('includeInvalid', 'true')
  const q = params.toString()
  return getJson(`/api/v1/classification/rules${q ? `?${q}` : ''}`)
}

export async function listCategoriesAdmin(includeDeleted = false) {
  const q = includeDeleted ? '?includeDeleted=true' : ''
  const raw = await getJson(`/api/v1/classification/categories${q}`)
  if (isCollectionResult<ConsumeCategoryRow>(raw)) return raw.rows
  return Array.isArray(raw) ? raw as ConsumeCategoryRow[] : []
}

export async function createRule(rule: ConsumeRuleRow) {
  return postJson('/api/v1/classification/rules', rule)
}

export type RuleHygieneSummary = {
  orphanCount: number
  archivedLegacyOrphanCount?: number
  activeInvalidPatternCount?: number
  archivedInvalidPatternCount?: number
  inactiveInvalidWithoutRemarkCount?: number
  recommendedKeywords: string[]
}

export async function fetchRuleHygiene(): Promise<RuleHygieneSummary> {
  return getJson('/api/v1/classification/rules/hygiene') as Promise<RuleHygieneSummary>
}

export async function fetchUnclassifiedRuleKeywords(limit = 20): Promise<string[]> {
  return getJson(`/api/v1/classification/rules/recommend-unclassified?limit=${limit}`) as Promise<string[]>
}

export async function updateRule(id: string, rule: ConsumeRuleRow) {
  return putJson(`/api/v1/classification/rules/${encodeURIComponent(id)}`, rule)
}

export async function deleteRule(id: string) {
  return deleteReq(`/api/v1/classification/rules/${encodeURIComponent(id)}`)
}

export async function createCategory(cat: ConsumeCategoryRow) {
  return postJson('/api/v1/classification/categories', cat)
}

export async function updateCategory(id: string, cat: ConsumeCategoryRow, cascade = false) {
  const q = cascade ? '?cascade=true' : ''
  return putJson(`/api/v1/classification/categories/${encodeURIComponent(id)}${q}`, cat)
}

export async function deleteCategory(id: string) {
  return deleteReq(`/api/v1/classification/categories/${encodeURIComponent(id)}`)
}

export type CategoryImpactPreview = {
  categoryId?: string
  categoryCode?: string
  categoryName?: string
  action?: string
  targetCode?: string
  targetName?: string
  transactionCount?: number
  totalAmount?: number
  activeRuleCount?: number
  inactiveRuleCount?: number
  childCategoryCount?: number
  amountByMonth?: Array<{ yearMonth: string; txnCount: number; amount: number }>
  affectedReports?: string[]
  warnings?: string[]
  summary?: string
}

export async function fetchCategoryImpactPreview(
  id: string,
  action: 'delete' | 'rename' | 'merge' = 'delete',
  targetCode?: string,
): Promise<CategoryImpactPreview> {
  const params = new URLSearchParams({ action })
  if (targetCode) params.set('targetCode', targetCode)
  return getJson(`/api/v1/classification/categories/${encodeURIComponent(id)}/impact-preview?${params}`) as Promise<CategoryImpactPreview>
}

export async function migrateCategory(
  id: string,
  toCode: string,
  deleteAfter = true,
  cascade = false,
) {
  const params = new URLSearchParams()
  params.set('toCode', toCode)
  if (deleteAfter) params.set('deleteAfter', 'true')
  if (cascade) params.set('cascade', 'true')
  return postJson(`/api/v1/classification/categories/${encodeURIComponent(id)}/migrate?${params}`, {})
}
