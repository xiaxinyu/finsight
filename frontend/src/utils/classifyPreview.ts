import type { ReclassifyPreviewRow } from '../api/transaction'
import type { TransactionRow } from '../api/transaction'

export function enrichPreviewRows(preview: ReclassifyPreviewRow[], selected: TransactionRow[]): ReclassifyPreviewRow[] {
  const byId = new Map(selected.map((r) => [r.id, r]))
  return preview.map((p) => {
    const row = byId.get(p.id)
    const beforeCode = p.beforeCategoryCode || row?.consumeCode || row?.consumeID || ''
    const beforeName = p.beforeCategoryName || row?.consumeName || ''
    return {
      ...p,
      transactionDesc: p.transactionDesc || row?.transactionDesc,
      transactionDate: p.transactionDate || row?.transactionDate,
      beforeCategoryCode: beforeCode || p.beforeCategoryCode,
      beforeCategoryName: beforeName || p.beforeCategoryName,
    }
  })
}

function manualRowFromTransaction(row: TransactionRow): ReclassifyPreviewRow {
  return {
    id: row.id,
    action: 'MANUAL',
    beforeCategoryCode: row.consumeCode || row.consumeID || '',
    beforeCategoryName: row.consumeName || '',
    transactionDesc: row.transactionDesc,
    transactionDate: row.transactionDate,
    reason: 'No auto match — pick a category manually',
  }
}

/** Merge API preview with selected ledger rows so manual override is always possible. */
export function buildClassifyPreviewRows(
  preview: ReclassifyPreviewRow[],
  selected: TransactionRow[],
): ReclassifyPreviewRow[] {
  const enriched = enrichPreviewRows(preview, selected)
  if (selected.length === 0) return enriched

  const seen = new Set(enriched.map((r) => r.id))
  const extras = selected
    .filter((r) => !seen.has(r.id))
    .map(manualRowFromTransaction)
  return [...enriched, ...extras]
}

export function hasManualClassifyRows(rows: ReclassifyPreviewRow[]): boolean {
  return rows.some((r) => r.action === 'MANUAL' || !r.categoryCode)
}

/** All rows need manual pick — optimize for one bulk category workflow. */
export function isBulkManualMode(rows: ReclassifyPreviewRow[], preview?: { classified?: number } | null): boolean {
  if (rows.length === 0) return false
  if ((preview?.classified ?? 0) > 0) return false
  return rows.every((r) => r.action === 'MANUAL' || !r.categoryCode)
}
