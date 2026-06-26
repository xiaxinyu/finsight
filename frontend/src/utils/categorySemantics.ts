export type CategorySemanticProfile = {
  reportRole?: string
  economicNature?: string
  budgetBehavior?: string
  fixedCostKind?: string | null
  semanticTag?: SemanticTagId
  includeInIncomeTrend?: boolean
  includeInExpenseTrend?: boolean
  includeInBudget?: boolean
  includeInFixedCostReport?: boolean
  includeInCashflow?: boolean
}

export type SemanticTagId =
  | 'real_income'
  | 'investment_income'
  | 'other_income'
  | 'refund_reimbursement'
  | 'daily_spending'
  | 'other_expense'
  | 'fixed_spending'
  | 'subscription_spending'
  | 'essential_spending'
  | 'transfer'
  | 'investment'
  | 'liability'
  | 'asset_adjustment'
  | 'other'

export type FixedCostKind =
  | 'rent'
  | 'utilities'
  | 'telecom'
  | 'insurance'
  | 'subscription'
  | 'education'
  | 'repayment'
  | 'other'

export type TxnTypeFilter = 'income' | 'expense' | 'both' | 'capital'

/** User-facing semantic tag labels — fallback when catalog API unavailable. */
export const SEMANTIC_TAG_LABELS: Record<SemanticTagId, string> = {
  real_income: 'Real Income',
  investment_income: 'Investment Income',
  other_income: 'Other Income',
  refund_reimbursement: 'Refund And Reimbursement',
  daily_spending: 'Variable Spending',
  other_expense: 'Other Expense',
  fixed_spending: 'Fixed Cost',
  subscription_spending: 'Subscription',
  essential_spending: 'Essential Expense',
  transfer: 'Transfer',
  investment: 'Investment Activity',
  liability: 'Debt And Repayment',
  asset_adjustment: 'Asset Adjustment',
  other: 'Unset',
}

export const FIXED_COST_KIND_LABELS: Record<FixedCostKind, string> = {
  rent: 'Rent And Mortgage',
  utilities: 'Utilities',
  telecom: 'Telecom And Internet',
  insurance: 'Insurance',
  subscription: 'Subscription',
  education: 'Education Fixed',
  repayment: 'Loan Repayment',
  other: 'Other Fixed',
}

export const SEMANTIC_TAG_GROUPS: Array<{ title: string; appliesTo: TxnTypeFilter; tags: SemanticTagId[] }> = [
  {
    title: 'Income Statement · Income',
    appliesTo: 'income',
    tags: ['real_income', 'investment_income', 'refund_reimbursement', 'other_income'],
  },
  {
    title: 'Income Statement · Expense',
    appliesTo: 'expense',
    tags: ['daily_spending', 'subscription_spending', 'essential_spending', 'other_expense'],
  },
  { title: 'Fixed Commitments', appliesTo: 'expense', tags: ['fixed_spending'] },
  {
    title: 'Capital And Transfers',
    appliesTo: 'capital',
    tags: ['transfer', 'investment', 'liability', 'asset_adjustment'],
  },
]

export const FIXED_COST_KIND_OPTIONS = Object.entries(FIXED_COST_KIND_LABELS).map(([value, label]) => ({
  value: value as FixedCostKind,
  label,
}))

const NATURE_LABELS: Record<string, string> = {
  income: 'Income',
  expense: 'Expense',
  refund: 'Refund',
  investment: 'Investment',
  liability: 'Liability',
  asset_adjustment: 'Asset Adjustment',
  transfer: 'Transfer',
  other: 'Other',
}

const BUDGET_BEHAVIOR_LABELS: Record<string, string> = {
  fixed: 'Fixed Cost',
  variable: 'Variable Spending',
  essential: 'Essential Expense',
  unclassified: 'Unclassified',
  none: 'Not Applicable',
}

export function semanticTagLabel(tag?: SemanticTagId): string {
  if (!tag) return '—'
  return SEMANTIC_TAG_LABELS[tag] ?? tag
}

export function fixedCostKindLabel(kind?: string | null): string {
  if (!kind) return ''
  return FIXED_COST_KIND_LABELS[kind as FixedCostKind] ?? kind
}

export function economicNatureLabel(nature?: string): string {
  if (!nature) return '—'
  return NATURE_LABELS[nature] ?? nature
}

export function budgetBehaviorLabel(behavior?: string): string {
  if (!behavior) return '—'
  return BUDGET_BEHAVIOR_LABELS[behavior] ?? behavior
}

export function isFixedCategory(parentId?: string, categoryCode?: string): boolean {
  if ((parentId ?? '').trim().toUpperCase() === 'FIXED') return true
  const code = (categoryCode ?? '').trim().toUpperCase()
  return code.startsWith('FIXED-') || code === 'FIXED'
}

export function inferFixedCostKind(parentId?: string, categoryCode?: string): FixedCostKind | null {
  if (!isFixedCategory(parentId, categoryCode)) return null
  const code = (categoryCode ?? '').trim().toUpperCase()
  const map: Record<string, FixedCostKind> = {
    'FIXED-01': 'rent',
    'FIXED-02': 'utilities',
    'FIXED-03': 'telecom',
    'FIXED-04': 'insurance',
    'FIXED-05': 'subscription',
    'FIXED-06': 'education',
    'FIXED-07': 'repayment',
    'FIXED-99': 'other',
  }
  if (map[code]) return map[code]
  return code.startsWith('FIXED-') ? 'other' : null
}

/** Infer stored report_role when DB value is missing (mirrors CategoryReportRoleInference). */
export function inferDefaultReportRole(
  parentId?: string,
  categoryCode?: string,
  txnTypes?: string,
): string {
  const parent = (parentId ?? '').trim().toUpperCase()
  const code = (categoryCode ?? '').trim().toUpperCase()
  const txn = (txnTypes ?? 'expense').toLowerCase()

  if (parent === 'INC' || parent === 'INCOME') {
    if (code.startsWith('INC-04') || txn.includes('invest')) return 'investment'
    if (code.startsWith('INC-08') || txn.includes('liability')) return 'liability'
    if (code.startsWith('INC-10') || txn.includes('refund')) return 'refund'
    if (txn.includes('income')) return 'income'
  }
  if (parent === 'REIM' || parent === 'REIMB') return 'refund'
  if (parent === 'ASSET') return txn.includes('transfer') ? 'transfer' : 'asset'
  if (parent === 'LIABILITY' || code.startsWith('DEBT-')) return 'liability'
  if (parent === 'INVEST' || parent === 'WEALTH' || parent === 'FP') return 'investment'
  if (parent === 'FE' || parent === 'FEE') return 'cashflow'
  if (parent === 'FIXED' || code.startsWith('FIXED-')) {
    if (code === 'FIXED-04') return 'cashflow'
    if (code === 'FIXED-07') return 'liability'
    return 'budget'
  }
  if (parent === 'GIFT' || parent === 'SOCIAL') {
    if (txn.includes('transfer')) return 'transfer'
    return 'budget'
  }
  if (txn.includes('income') && !txn.includes('expense')) return 'income'
  if (txn.includes('expense')) return 'budget'
  return 'budget'
}

export function txnTypeFilter(txnTypes?: string): TxnTypeFilter {
  const txn = (txnTypes ?? '').toLowerCase()
  if (txn.includes('income') && txn.includes('expense')) return 'both'
  if (txn.includes('income')) return 'income'
  if (txn.includes('expense')) return 'expense'
  return 'both'
}

export function filterSemanticTagGroups(
  groups: Array<{ title: string; appliesTo?: string; tags: SemanticTagId[] }>,
  txnTypes?: string,
) {
  const filter = txnTypeFilter(txnTypes)
  return groups.filter((g) => {
    const applies = g.appliesTo ?? 'both'
    if (filter === 'both') return true
    if (applies === 'both' || applies === 'capital') return true
    if (filter === 'income') return applies === 'income'
    if (filter === 'expense') return applies === 'expense'
    return true
  })
}

export function semanticTagFromReportRole(
  reportRole?: string,
  parentId?: string,
  categoryCode?: string,
  txnTypes?: string,
): SemanticTagId {
  const role = (reportRole?.trim() || inferDefaultReportRole(parentId, categoryCode, txnTypes)).toLowerCase()
  const parent = (parentId ?? '').trim().toUpperCase()
  const code = (categoryCode ?? '').trim().toUpperCase()

  if (role === 'income') {
    if (code.startsWith('INC-04') || parent === 'INVEST') return 'investment_income'
    if (code === 'INC-99') return 'other_income'
    return 'real_income'
  }
  if (role === 'refund') return 'refund_reimbursement'
  if (role === 'transfer') return 'transfer'
  if (role === 'investment') {
    if (parent === 'INC' || parent === 'INCOME' || code.startsWith('INC-04')) return 'investment_income'
    return 'investment'
  }
  if (role === 'liability') {
    return isFixedCategory(parentId, categoryCode) ? 'fixed_spending' : 'liability'
  }
  if (role === 'asset') return 'asset_adjustment'
  if (role === 'cashflow') {
    return isFixedCategory(parentId, categoryCode) ? 'fixed_spending' : 'essential_spending'
  }
  if (role === 'budget') {
    const kind = inferFixedCostKind(parentId, categoryCode)
    if (kind === 'subscription') return 'subscription_spending'
    if (isFixedCategory(parentId, categoryCode)) return 'fixed_spending'
    if (code.startsWith('OTHER') || code === 'OTHER') return 'other_expense'
    return 'daily_spending'
  }
  if (role === 'other') return 'other'
  return 'other'
}

export function reportRoleFromSemanticSelection(
  tag: SemanticTagId,
  fixedKind?: FixedCostKind | null,
): string {
  switch (tag) {
    case 'real_income':
    case 'investment_income':
    case 'other_income':
      return tag === 'investment_income' ? 'investment' : 'income'
    case 'refund_reimbursement': return 'refund'
    case 'daily_spending':
    case 'other_expense':
      return 'budget'
    case 'subscription_spending': return 'budget'
    case 'fixed_spending':
      if (fixedKind === 'insurance') return 'cashflow'
      if (fixedKind === 'repayment') return 'liability'
      return 'budget'
    case 'essential_spending': return 'cashflow'
    case 'transfer': return 'transfer'
    case 'investment': return 'investment'
    case 'liability': return 'liability'
    case 'asset_adjustment': return 'asset'
    default: return 'other'
  }
}

export function inclusionSummary(p: CategorySemanticProfile): string {
  const parts: string[] = []
  if (p.includeInIncomeTrend) parts.push('Income Trend')
  if (p.includeInExpenseTrend) parts.push('Expense Trend')
  if (p.includeInBudget) parts.push('Budget')
  if (p.includeInFixedCostReport) parts.push('Fixed Cost Report')
  if (p.includeInCashflow) parts.push('Cash Flow')
  if (!parts.length) parts.push('Excluded From Core Reports')
  return parts.join(' · ')
}

export function profileCategorySemantics(
  reportRole?: string,
  txnTypes?: string,
  parentId?: string,
  categoryCode?: string,
): CategorySemanticProfile {
  const role = (reportRole?.trim() || inferDefaultReportRole(parentId, categoryCode, txnTypes)).toLowerCase()
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
  const semanticTag = semanticTagFromReportRole(role, parentId, categoryCode, txnTypes)
  const includeInIncomeTrend = role === 'income' && txn.includes('income')
  const includeInExpenseTrend = !excluded
    && txn.includes('expense')
    && (role === 'budget' || role === 'cashflow' || role === 'other')
  const includeInBudget = !excluded
    && txn.includes('expense')
    && (role === 'budget' || role === 'cashflow')
  const includeInCashflow = role !== 'transfer'

  let budgetBehavior = 'variable'
  if (role === 'income' || excluded) budgetBehavior = 'none'
  else if (semanticTag === 'subscription_spending') budgetBehavior = 'fixed'
  else if (isFixedCategory(parentId, categoryCode) && (role === 'budget' || role === 'cashflow')) {
    budgetBehavior = 'fixed'
  } else if (role === 'cashflow') budgetBehavior = 'essential'
  else if (role === 'budget' && txn.includes('expense')) budgetBehavior = 'variable'
  else if (role === 'other') budgetBehavior = 'unclassified'

  const fixedCostKind = inferFixedCostKind(parentId, categoryCode)
    ?? (semanticTag === 'subscription_spending' ? 'subscription' as FixedCostKind : null)

  const isFixedTag = semanticTag === 'fixed_spending' || semanticTag === 'subscription_spending'

  return {
    reportRole: role,
    economicNature,
    budgetBehavior,
    fixedCostKind,
    semanticTag,
    includeInIncomeTrend,
    includeInExpenseTrend,
    includeInBudget,
    includeInFixedCostReport: isFixedTag || budgetBehavior === 'fixed',
    includeInCashflow,
  }
}

export function isFixedCostCategoryCode(categoryCode?: string): boolean {
  const code = (categoryCode ?? '').trim().toUpperCase()
  return code.startsWith('FIXED-') || code === 'FIXED'
}

/** @deprecated Use catalog API */
export const REPORT_ROLE_OPTIONS: Array<{ value: string; label: string }> = []

export function reportRoleLabel(_role?: string): string {
  return '—'
}
