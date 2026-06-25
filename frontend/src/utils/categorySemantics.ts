export type CategorySemanticProfile = {
  reportRole?: string
  economicNature?: string
  budgetBehavior?: string
  includeInIncomeTrend?: boolean
  includeInExpenseTrend?: boolean
  includeInBudget?: boolean
}

const ROLE_LABELS: Record<string, string> = {
  income: 'Income trend',
  refund: 'Refund / reimbursement',
  budget: 'Budget spending',
  cashflow: 'Cashflow / essential',
  investment: 'Investment',
  liability: 'Liability',
  asset: 'Asset adjustment',
  transfer: 'Transfer',
  other: 'Other / unset',
}

/** Allowed report_role values (matches cls_category.report_role). */
export const REPORT_ROLE_OPTIONS = Object.entries(ROLE_LABELS).map(([value, label]) => ({
  value,
  label,
}))

const NATURE_LABELS: Record<string, string> = {
  income: 'Income',
  expense: 'Expense',
  refund: 'Refund',
  investment: 'Investment',
  liability: 'Liability',
  asset_adjustment: 'Asset adjustment',
  transfer: 'Transfer',
  other: 'Other',
}

export function reportRoleLabel(role?: string): string {
  if (!role) return '—'
  return ROLE_LABELS[role] ?? role
}

export function economicNatureLabel(nature?: string): string {
  if (!nature) return '—'
  return NATURE_LABELS[nature] ?? nature
}

export function inclusionSummary(p: CategorySemanticProfile): string {
  const parts: string[] = []
  if (p.includeInIncomeTrend) parts.push('income trend')
  if (p.includeInExpenseTrend) parts.push('expense trend')
  if (p.includeInBudget) parts.push('budget')
  if (!parts.length) parts.push('excluded from income/expense/budget trends')
  return parts.join(' · ')
}

/** Preview finance semantics from report role + txn types (mirrors CategoryFinanceSemantics.java). */
export function profileCategorySemantics(reportRole?: string, txnTypes?: string): CategorySemanticProfile {
  const role = (reportRole?.trim() || 'other').toLowerCase()
  const txn = (txnTypes ?? '').toLowerCase()
  let economicNature = 'other'
  if (role === 'income') economicNature = 'income'
  else if (role === 'refund') economicNature = 'refund'
  else if (role === 'investment') economicNature = 'investment'
  else if (role === 'liability') economicNature = 'liability'
  else if (role === 'asset') economicNature = 'asset_adjustment'
  else if (role === 'transfer') economicNature = 'transfer'
  else if (role === 'budget' || role === 'cashflow') economicNature = 'expense'

  const excluded = role === 'transfer' || role === 'refund' || role === 'investment'
    || role === 'liability' || role === 'asset'
  const includeInIncomeTrend = role === 'income' && txn.includes('income')
  const includeInExpenseTrend = !excluded
    && txn.includes('expense')
    && (role === 'budget' || role === 'cashflow' || role === 'other')
  const includeInBudget = !excluded
    && txn.includes('expense')
    && (role === 'budget' || role === 'cashflow')

  return {
    reportRole: role,
    economicNature,
    includeInIncomeTrend,
    includeInExpenseTrend,
    includeInBudget,
  }
}
