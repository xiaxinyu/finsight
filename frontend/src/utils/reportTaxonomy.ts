import type { SemanticsCatalog } from '../api/admin'
import { SEMANTIC_TAG_LABELS, type SemanticTagId } from './categorySemantics'

/** Standard report column labels — aligned with admin taxonomy. */
export const REPORT_COLUMN_LABELS = {
  classification: 'Reporting Classification',
  txnType: 'Transaction type',
  classL1: 'Group',
  classL2: 'Tag',
  amount: 'Amount',
  sharePct: 'Share',
} as const

/** Transaction type labels shown in report tables (plain English — no accounting jargon). */
export type ReportTxnTypeLabel =
  | 'Income'
  | 'Expense'
  | 'Tax'
  | 'Refund'
  | 'Transfer'
  | 'Finance'
  | 'Investment'

export const REPORT_TXN_TYPE_LABELS: ReportTxnTypeLabel[] = [
  'Income',
  'Expense',
  'Tax',
  'Refund',
  'Transfer',
  'Finance',
  'Investment',
]

export type SemanticBreakdownScope =
  | 'expense'
  | 'income'
  | 'non_pnl'
  | 'tax'
  | 'refund'
  | 'all'

export const SEMANTIC_BREAKDOWN_SCOPES: Array<{ value: SemanticBreakdownScope; label: string; hint: string }> = [
  { value: 'expense', label: 'Spending', hint: 'Consumption and fixed costs in expense trend' },
  { value: 'income', label: 'Income', hint: 'Earned and portfolio income in income trend' },
  { value: 'non_pnl', label: 'Transfer & Finance', hint: 'Account transfers, loans, and investments — excluded from spending' },
  { value: 'tax', label: 'Tax', hint: 'Statutory tax paid and refunds' },
  { value: 'refund', label: 'Refund', hint: 'Reimbursements and consumer refunds' },
  { value: 'all', label: 'All classified', hint: 'Every tagged flow in the period' },
]

export const REPORT_METRICS_SOURCE = 'v_transaction_finance_semantics.semantic_tag'

/** Coarse filters for Transactions — map to SQL predicates in TransactionMapper. */
export const REPORTING_CLASSIFICATION_QUICK_FILTERS = [
  { value: 'consumption', label: 'All spending' },
  { value: 'real_income', label: 'All income' },
  { value: 'refund', label: 'Refunds' },
  { value: 'transfer', label: 'Transfers' },
  { value: 'investment', label: 'Investments' },
  { value: 'liability', label: 'Loans & debt' },
  { value: 'data_quality', label: 'Data quality issues' },
] as const

export type ReportingClassificationFilterOption = {
  value: string
  label: string
  group: string
}

export function buildReportingClassificationFilterOptions(
  catalog?: SemanticsCatalog,
): ReportingClassificationFilterOption[] {
  const out: ReportingClassificationFilterOption[] = REPORTING_CLASSIFICATION_QUICK_FILTERS.map((f) => ({
    ...f,
    group: 'Quick filters',
  }))

  const groups = catalog?.semanticTagGroups ?? []
  for (const g of groups) {
    for (const tagId of g.tags) {
      const label = catalog?.semanticTags?.[tagId]?.label
        ?? SEMANTIC_TAG_LABELS[tagId as SemanticTagId]
        ?? tagId
      out.push({ value: tagId, label, group: g.title })
    }
  }
  return out
}

export function reportingClassificationFilterSelectOptions(
  options: ReportingClassificationFilterOption[],
): Array<{ label: string; options: Array<{ value: string; label: string }> }> {
  const byGroup = new Map<string, Array<{ value: string; label: string }>>()
  for (const opt of options) {
    const list = byGroup.get(opt.group) ?? []
    list.push({ value: opt.value, label: opt.label })
    byGroup.set(opt.group, list)
  }
  return [...byGroup.entries()].map(([label, opts]) => ({ label, options: opts }))
}

export function reportingClassificationFilterLabel(
  value: string,
  catalog?: SemanticsCatalog,
): string {
  const quick = REPORTING_CLASSIFICATION_QUICK_FILTERS.find((f) => f.value === value)
  if (quick) return quick.label
  return catalog?.semanticTags?.[value]?.label
    ?? SEMANTIC_TAG_LABELS[value as SemanticTagId]
    ?? value
}
