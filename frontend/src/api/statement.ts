import { getJson, postCommon, uploadFile } from './client'
import { isCollectionResult } from './normalize'

export async function listStatements(page = 1, rows = 20) {
  const raw = await getJson(`/statement/api/list?page=${page}&rows=${rows}`)
  if (isCollectionResult(raw)) return raw
  return { total: 0, rows: [] }
}

export async function uploadStatement(file: File, bankCode?: string, cardTypeCode?: string, cardNo?: string) {
  const fd = new FormData()
  fd.append('file', file)
  if (bankCode) fd.append('bankCode', bankCode)
  if (cardTypeCode) fd.append('cardTypeCode', cardTypeCode)
  if (cardNo) fd.append('cardNo', cardNo)
  return uploadFile('/statement/upload', fd)
}

export async function commitStatement(statementId: string) {
  return postCommon('/statement/commit', { statementId })
}

export async function previewStatement(statementId: string) {
  return getJson(`/statement/preview?statementId=${encodeURIComponent(statementId)}`)
}
