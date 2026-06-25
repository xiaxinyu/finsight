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
