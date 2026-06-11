import { ApiError, getJson, postCommon, uploadFile } from './client'
import { isCollectionResult, parseJsonObject } from './normalize'

export interface StatementUploadResult {
  statementId: string
  rows: number
  parsed: number
  skipped: number
  /** Header/metadata/merged lines not shown in Skipped tab */
  ignored?: number
  /** Source lines linked to a parsed transaction (may exceed parsed when rows merge) */
  linked?: number
  bankCardId?: string
  bankCardName?: string
}

export interface StatementCommitResult {
  statementId: string
  total: number
  imported: number
  failed: number
  skippedDuplicates?: number
}

export interface SkippedImportRow {
  lineNumber: number
  fileLineNumber?: number
  rawText: string
  originalLine?: string
  columns?: string[]
  reason: string
  hint?: string
  contextBefore?: string
  contextAfter?: string
}

export interface StatementListRow {
  id: string
  fileName?: string
  itemCount?: number
  rowCount?: number
  status?: string
  sourceBankCode?: string
  /** Legacy alias from API */
  source?: string
  createdAt?: string
  createTime?: string
  createtime?: string
}

export interface StatementPreviewRow {
  id: string
  transactionDate?: string
  bookKeepingDate?: string
  transactionDesc?: string
  balanceMoney?: number
  incomeMoney?: number
  accountBalance?: number
  cardTypeName?: string
  bankCardId?: string
  bankCardName?: string
  consumeName?: string
  opponentName?: string
  opponentAccount?: string
  demoArea?: string
  possibleDuplicate?: boolean
  txnType?: string
}

export async function listStatements(page = 1, rows = 20) {
  const raw = await getJson(`/statement/api/list?page=${page}&rows=${rows}`)
  if (isCollectionResult(raw)) return raw
  return { total: 0, rows: [] }
}

function parseStatementPayload<T>(raw: unknown): T {
  const parsed = parseJsonObject(raw)
  if (parsed) return parsed as T
  if (raw && typeof raw === 'object' && !Array.isArray(raw)) return raw as T
  throw new ApiError('Unexpected server response — check login session', 500)
}

export async function uploadStatement(
  file: File,
  bankCode?: string,
  cardTypeCode?: string,
  cardNo?: string,
  bankCardId?: string,
) {
  const fd = new FormData()
  fd.append('file', file)
  if (bankCode) fd.append('bankCode', bankCode)
  if (cardTypeCode) fd.append('cardTypeCode', cardTypeCode)
  if (cardNo) fd.append('cardNo', cardNo)
  if (bankCardId) fd.append('bankCardId', bankCardId)
  const raw = await uploadFile('/statement/upload', fd)
  const result = parseStatementPayload<StatementUploadResult>(raw)
  if (!result.statementId) throw new ApiError('Upload succeeded but statement id is missing', 500)
  return result
}

export async function commitStatement(statementId: string) {
  const raw = await postCommon('/statement/commit', { statementId })
  return parseStatementPayload<StatementCommitResult>(raw)
}

export async function previewStatement(statementId: string) {
  const raw = await getJson(`/statement/preview?statementId=${encodeURIComponent(statementId)}`)
  return Array.isArray(raw) ? (raw as StatementPreviewRow[]) : []
}

export type StatementSourceLineKind = 'linked' | 'skipped' | 'ignored' | 'header' | 'noise'

export interface StatementSourceLineRow {
  lineNumber: number
  fileLineNumber: number
  originalLine?: string
  columns?: string[]
  kind: StatementSourceLineKind
  reason?: string
  hint?: string
}

export interface StatementSourceView {
  statementId?: string
  fileName?: string
  bankCode?: string
  columnHeaders?: string[]
  lines?: number
  transactions?: number
  linked?: number
  skipped?: number
  ignored?: number
  rows?: StatementSourceLineRow[]
}

export async function fetchStatementSourceLines(statementId: string, cardTypeCode = 'debit') {
  const raw = await getJson(
    `/statement/source-lines?statementId=${encodeURIComponent(statementId)}&cardTypeCode=${encodeURIComponent(cardTypeCode)}`,
  )
  if (raw && typeof raw === 'object' && !Array.isArray(raw)) return raw as StatementSourceView
  return { rows: [] } as StatementSourceView
}

export async function skippedStatementLines(statementId: string, cardTypeCode?: string) {
  const card = cardTypeCode ? `&cardTypeCode=${encodeURIComponent(cardTypeCode)}` : ''
  const raw = await getJson(`/statement/skipped-lines?statementId=${encodeURIComponent(statementId)}${card}`)
  return Array.isArray(raw) ? (raw as SkippedImportRow[]) : []
}
