import {
  type FixedCostKind,
  fixedKindFromFlatTag,
  flatFixedTagForKind,
  isAnyFixedSemanticTag,
  isFlatFixedSemanticTag,
  normalizeFixedSemanticTag,
} from './categoryFixedTags'

export type { FixedCostKind } from './categoryFixedTags'
export {
  isAnyFixedSemanticTag,
  isFlatFixedSemanticTag,
  normalizeFixedSemanticTag,
  flatFixedTagForKind,
  fixedKindFromFlatTag,
} from './categoryFixedTags'

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
  | 'dining_spending'
  | 'shopping_spending'
  | 'transport_spending'
  | 'entertainment_spending'
  | 'education_spending'
  | 'medical_spending'
  | 'social_spending'
  | 'other_expense'
  | 'fixed_spending'
  | 'fixed_housing'
  | 'fixed_utilities'
  | 'fixed_telecom'
  | 'fixed_insurance'
  | 'fixed_tuition'
  | 'fixed_repayment'
  | 'fixed_misc'
  | 'subscription_spending'
  | 'essential_spending'
  | 'transfer'
  | 'investment'
  | 'liability'
  | 'asset_adjustment'
  | 'other'

export type TxnTypeFilter = 'income' | 'expense' | 'both' | 'capital'

/** User-facing semantic tag labels — fallback when catalog API unavailable. */
export const SEMANTIC_TAG_LABELS: Record<SemanticTagId, string> = {
  real_income: 'Earned',
  investment_income: 'Portfolio',
  other_income: 'MiscIncome',
  refund_reimbursement: 'Refund',
  daily_spending: 'General',
  dining_spending: 'Dining',
  shopping_spending: 'Shopping',
  transport_spending: 'Transport',
  entertainment_spending: 'Entertainment',
  education_spending: 'Education',
  medical_spending: 'Medical',
  social_spending: 'Social',
  other_expense: 'MiscExpense',
  fixed_spending: 'Fixed',
  fixed_housing: 'Housing',
  fixed_utilities: 'Utilities',
  fixed_telecom: 'Telecom',
  fixed_insurance: 'Insurance',
  fixed_tuition: 'Tuition',
  fixed_repayment: 'Repayment',
  fixed_misc: 'FixedOther',
  subscription_spending: 'Subscription',
  essential_spending: 'Essential',
  transfer: 'Transfer',
  investment: 'Investment',
  liability: 'Debt',
  asset_adjustment: 'Rebalance',
  other: 'Unset',
}

export const FIXED_COST_KIND_LABELS: Record<FixedCostKind, string> = {
  rent: 'Housing',
  utilities: 'Utilities',
  telecom: 'Telecom',
  insurance: 'Insurance',
  subscription: 'Subscription',
  education: 'Education',
  repayment: 'Repayment',
  other: 'Other',
}

export const SEMANTIC_TAG_GROUPS: Array<{ title: string; appliesTo: TxnTypeFilter; tags: SemanticTagId[] }> = [
  {
    title: 'Income',
    appliesTo: 'income',
    tags: ['real_income', 'investment_income', 'refund_reimbursement', 'other_income'],
  },
  {
    title: 'Expense',
    appliesTo: 'expense',
    tags: [
      'dining_spending',
      'shopping_spending',
      'transport_spending',
      'entertainment_spending',
      'education_spending',
      'medical_spending',
      'social_spending',
      'subscription_spending',
      'essential_spending',
      'daily_spending',
      'other_expense',
    ],
  },
  {
    title: 'Fixed',
    appliesTo: 'expense',
    tags: [
      'fixed_housing',
      'fixed_utilities',
      'fixed_telecom',
      'fixed_insurance',
      'fixed_tuition',
      'fixed_repayment',
      'fixed_misc',
    ],
  },
  {
    title: 'Capital',
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
  fixed: 'Fixed',
  variable: 'Discretionary',
  essential: 'Essential',
  unclassified: 'Unclassified',
  none: 'N/A',
}

export function semanticTagLabel(tag?: SemanticTagId): string {
  if (!tag) return '—'
  return SEMANTIC_TAG_LABELS[tag] ?? tag
}

export function isKnownSemanticTag(tag?: string | null): tag is SemanticTagId {
  if (!tag?.trim()) return false
  return tag.trim() in SEMANTIC_TAG_LABELS
}

/** Prefer stored semantic tag; fall back to report_role + category tree inference. */
export function resolveSemanticTag(
  storedTag?: string | null,
  reportRole?: string,
  parentId?: string,
  categoryCode?: string,
  txnTypes?: string,
  categoryName?: string,
): SemanticTagId {
  if (isKnownSemanticTag(storedTag)) {
    const raw = storedTag.trim() as SemanticTagId
    if (raw === 'fixed_spending') {
      return normalizeFixedSemanticTag(raw, inferFixedCostKind(parentId, categoryCode)) as SemanticTagId
    }
    return raw
  }
  const role = effectiveReportRole(reportRole, txnTypes, parentId, categoryCode)
  return semanticTagFromReportRole(role, parentId, categoryCode, txnTypes, categoryName)
}

export function reportRoleForSemanticTag(
  tag: SemanticTagId,
  parentId?: string,
  categoryCode?: string,
  fixedKind?: FixedCostKind | null,
): string {
  const flatKind = fixedKindFromFlatTag(tag)
  if (flatKind) {
    return reportRoleFromSemanticSelection(tag, flatKind)
  }
  let kind = fixedKind
  if (tag === 'fixed_spending' && !kind) {
    kind = inferFixedCostKind(parentId, categoryCode)
  }
  return reportRoleFromSemanticSelection(tag, kind)
}

/** Build persisted category fields from explicit semantic tag selection. */
export function categoryFieldsFromSemanticTag(
  semanticTag: SemanticTagId,
  fields: {
    parentId?: string
    code?: string
    txnTypes?: string
  },
): { semanticTag: SemanticTagId; reportRole: string; txnTypes: string; warnings: string[] } {
  const warnings: string[] = []
  let txnTypes = (fields.txnTypes ?? 'expense').trim() || 'expense'
  const parentId = fields.parentId
  const code = fields.code

  if (isFixedCategory(parentId, code) && !txnTypes.includes('expense')) {
    txnTypes = 'expense'
    warnings.push('Fixed categories use Transaction Type Expense — adjusted automatically.')
  }

  const fixedKind = isFlatFixedSemanticTag(semanticTag)
    ? fixedKindFromFlatTag(semanticTag)
    : semanticTag === 'fixed_spending'
      ? inferFixedCostKind(parentId, code)
      : null
  const reportRole = reportRoleForSemanticTag(semanticTag, parentId, code, fixedKind)
  return { semanticTag, reportRole, txnTypes, warnings }
}

/** Initial form values when loading a category row from API. */
export function initialCategoryFormValues(
  row: {
    name?: string
    parentId?: string
    code?: string
    txnTypes?: string
    reportRole?: string
    semanticTag?: string
  },
): { semanticTag: SemanticTagId; reportRole: string; txnTypes: string } {
  const txnTypes = (row.txnTypes ?? 'expense').trim() || 'expense'
  const semanticTag = resolveSemanticTag(
    row.semanticTag,
    row.reportRole,
    row.parentId,
    row.code,
    txnTypes,
    row.name,
  )
  const derived = categoryFieldsFromSemanticTag(semanticTag, {
    parentId: row.parentId,
    code: row.code,
    txnTypes,
  })
  return {
    semanticTag,
    reportRole: derived.reportRole,
    txnTypes: derived.txnTypes,
  }
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

export function isSocialCategory(parentId?: string, categoryCode?: string): boolean {
  const parent = (parentId ?? '').trim().toUpperCase()
  if (parent === 'GIFT' || parent === 'SOCIAL') return true
  const code = (categoryCode ?? '').trim().toUpperCase()
  return code.startsWith('GIFT-') || code.startsWith('SOCIAL-')
}

/** Expense domain tag from category tree (name/code/parent). */
export function inferExpenseDomainTag(
  parentId?: string,
  categoryCode?: string,
  categoryName?: string,
): SemanticTagId {
  const parent = (parentId ?? '').trim().toUpperCase()
  const code = (categoryCode ?? '').trim().toUpperCase()
  const name = categoryName ?? ''

  if (code.startsWith('DAILY-05') || code.startsWith('LIVING-06')) return 'medical_spending'
  if (code.startsWith('DAILY-01') || code.startsWith('DAILY-02')) return 'dining_spending'
  if (code.startsWith('DAILY-03') || code.startsWith('DAILY-04') || code.startsWith('SHOP-')) return 'shopping_spending'
  if (code.startsWith('TRANS-') || code.startsWith('TRAVEL-')) return 'transport_spending'
  if (code.startsWith('ENT-')) return 'entertainment_spending'
  if (code.startsWith('EDU-') && code !== 'EDU-01') return 'education_spending'

  if (/超市|购物|网上|电商|服饰|美妆|母婴|家居|耐用品|日用品|百货/.test(name)) return 'shopping_spending'
  if (/交通|地铁|公交|打车|网约车|滴滴|停车|油费|充电|过路|车辆|机票|火车|租车|代驾|保养|洗车/.test(name)) {
    return 'transport_spending'
  }
  if (/餐饮|外卖|堂食|早餐|咖啡|饭店|吃饭|小吃/.test(name)) return 'dining_spending'
  if (/娱乐|旅行|旅游|酒店|景点|门票|电影|演出|游戏|健身|运动/.test(name)) return 'entertainment_spending'
  if (/培训|书籍|资料|课程/.test(name)) return 'education_spending'
  if (/医疗|医院|药|体检|挂号|牙科|疫苗/.test(name)) return 'medical_spending'
  if (/宠物|家政|快递|保洁|维修/.test(name)) return 'daily_spending'

  if (parent === 'SHOPPING' || parent === 'SHOP') return 'shopping_spending'
  if (parent === 'TRANSPORT' || parent === 'TRAVEL') return 'transport_spending'
  if (parent === 'ENT') return 'entertainment_spending'
  if (parent === 'EDU') return 'education_spending'
  if (parent === 'LIVING') return 'daily_spending'
  return 'daily_spending'
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
  parentId?: string,
  categoryCode?: string,
) {
  const filter = txnTypeFilter(txnTypes)
  const parent = (parentId ?? '').trim().toUpperCase()
  const code = (categoryCode ?? '').trim().toUpperCase()

  if (parent === 'REIM' || parent === 'REIMB' || code.startsWith('REIM-')) {
    return groups.filter((g) => g.appliesTo === 'income')
  }
  if (parent === 'FIXED' || code.startsWith('FIXED-')) {
    return groups.filter((g) => g.title === 'Fixed' || g.appliesTo === 'capital')
  }
  if ((parent === 'INC' || parent === 'INCOME') && filter === 'income') {
    return groups.filter((g) => g.appliesTo === 'income')
  }
  if ((parent === 'ASSET' || parent === 'LIABILITY' || parent === 'INVEST' || parent === 'WEALTH')
    && filter === 'capital') {
    return groups.filter((g) => g.appliesTo === 'capital')
  }

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
  categoryName?: string,
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
    if (isFixedCategory(parentId, categoryCode)) {
      return flatFixedTagForKind(inferFixedCostKind(parentId, categoryCode) ?? 'repayment')
    }
    return 'liability'
  }
  if (role === 'asset') return 'asset_adjustment'
  if (role === 'cashflow') {
    if (isFixedCategory(parentId, categoryCode)) {
      const kind = inferFixedCostKind(parentId, categoryCode)
      if (kind === 'insurance') return 'fixed_insurance'
      return flatFixedTagForKind(kind)
    }
    return 'essential_spending'
  }
  if (role === 'budget') {
    const kind = inferFixedCostKind(parentId, categoryCode)
    if (kind === 'subscription') return 'subscription_spending'
    if (isFixedCategory(parentId, categoryCode)) {
      return flatFixedTagForKind(kind)
    }
    if (isSocialCategory(parentId, categoryCode)) return 'social_spending'
    if (code.startsWith('OTHER') || code === 'OTHER') return 'other_expense'
    return inferExpenseDomainTag(parentId, categoryCode, categoryName)
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
    case 'dining_spending':
    case 'shopping_spending':
    case 'transport_spending':
    case 'entertainment_spending':
    case 'education_spending':
    case 'medical_spending':
    case 'social_spending':
    case 'other_expense':
      return 'budget'
    case 'subscription_spending': return 'budget'
    case 'fixed_housing':
    case 'fixed_utilities':
    case 'fixed_telecom':
    case 'fixed_tuition':
    case 'fixed_misc':
      return 'budget'
    case 'fixed_insurance':
      return 'cashflow'
    case 'fixed_repayment':
      return 'liability'
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
  if (p.includeInIncomeTrend) parts.push('IncomeTrend')
  if (p.includeInExpenseTrend) parts.push('ExpenseTrend')
  if (p.includeInBudget) parts.push('Budget')
  if (p.includeInFixedCostReport) parts.push('FixedCost')
  if (p.includeInCashflow) parts.push('CashFlow')
  if (!parts.length) parts.push('Excluded')
  return parts.join(' · ')
}

export function profileCategorySemantics(
  reportRole?: string,
  txnTypes?: string,
  parentId?: string,
  categoryCode?: string,
  storedSemanticTag?: string | null,
): CategorySemanticProfile {
  const role = effectiveReportRole(reportRole, txnTypes, parentId, categoryCode).toLowerCase()
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
  const semanticTag = resolveSemanticTag(storedSemanticTag, role, parentId, categoryCode, txnTypes)
  const includeInIncomeTrend = role === 'income' && txn.includes('income')
  const includeInExpenseTrend = !excluded
    && txn.includes('expense')
    && (role === 'budget' || role === 'cashflow' || role === 'other')
  const includeInBudget = !excluded
    && txn.includes('expense')
    && (role === 'budget' || role === 'cashflow')
  const includeInCashflow = role !== 'transfer'
    && (includeInIncomeTrend || includeInExpenseTrend || role === 'investment' || role === 'liability')

  let budgetBehavior = 'variable'
  if (role === 'income' || excluded) budgetBehavior = 'none'
  else if (semanticTag === 'subscription_spending') budgetBehavior = 'fixed'
  else if (isAnyFixedSemanticTag(semanticTag) && (role === 'budget' || role === 'cashflow' || role === 'liability')) {
    budgetBehavior = 'fixed'
  } else if (role === 'cashflow') budgetBehavior = 'essential'
  else if (role === 'budget' && txn.includes('expense')) budgetBehavior = 'variable'
  else if (role === 'other') budgetBehavior = 'unclassified'

  const fixedCostKind = fixedKindFromFlatTag(semanticTag)
    ?? inferFixedCostKind(parentId, categoryCode)
    ?? (semanticTag === 'subscription_spending' ? 'subscription' as FixedCostKind : null)

  const isFixedTag = isAnyFixedSemanticTag(semanticTag) || semanticTag === 'subscription_spending'

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

export function effectiveReportRole(
  reportRole?: string,
  txnTypes?: string,
  parentId?: string,
  categoryCode?: string,
): string {
  return coerceCategoryFormFields({
    parentId,
    code: categoryCode,
    txnTypes,
    reportRole,
  }).reportRole
}

export function parentTxnMismatchWarning(parentId?: string, txnTypes?: string): string | null {
  const parent = (parentId ?? '').trim().toUpperCase()
  const filter = txnTypeFilter(txnTypes)
  if ((parent === 'INC' || parent === 'INCOME') && filter === 'expense') {
    return 'Parent is an income group but transaction type is Expense — verify the parent category.'
  }
  if (parent === 'FIXED' && filter === 'income') {
    return 'Fixed categories should use Transaction Type Expense.'
  }
  return null
}

/** Short group titles for admin UI (no "Income Statement" prefix on expense side). */
export function compactGroupTitle(title: string, txnFilter: ReturnType<typeof txnTypeFilter>): string {
  if (txnFilter === 'expense') {
    return title
      .replace(/^Income Statement · Expense$/i, 'Expense')
      .replace(/^Fixed Commitments$/i, 'Fixed')
      .replace(/^Capital And Transfers$/i, 'Capital')
  }
  if (txnFilter === 'income') {
    return title.replace(/^Income Statement · Income$/i, 'Income')
  }
  return title
    .replace(/^Income Statement · Income$/i, 'Income')
    .replace(/^Income Statement · Expense$/i, 'Expense')
    .replace(/^Fixed Commitments$/i, 'Fixed')
    .replace(/^Capital And Transfers$/i, 'Capital')
}

export type CoercedCategoryFields = {
  txnTypes: string
  reportRole: string
  warnings: string[]
}

/** Align txn type + report role with parent/category conventions before save or display. */
export function coerceCategoryFormFields(fields: {
  parentId?: string
  code?: string
  txnTypes?: string
  reportRole?: string
}): CoercedCategoryFields {
  const warnings: string[] = []
  let txnTypes = (fields.txnTypes ?? 'expense').trim() || 'expense'
  const parentId = fields.parentId
  const code = fields.code

  if (isFixedCategory(parentId, code) && !txnTypes.includes('expense')) {
    txnTypes = 'expense'
    warnings.push('Fixed categories use Transaction Type Expense — adjusted automatically.')
  }

  let reportRole = fields.reportRole?.trim()
    || inferDefaultReportRole(parentId, code, txnTypes)

  if (isFixedCategory(parentId, code)) {
    const kind = inferFixedCostKind(parentId, code) ?? 'rent'
    if (kind === 'subscription') {
      reportRole = reportRoleFromSemanticSelection('subscription_spending', 'subscription')
    } else {
      reportRole = reportRoleFromSemanticSelection(flatFixedTagForKind(kind), kind)
    }
  } else if (txnTypes === 'income' && !txnTypes.includes('expense')) {
    const incompatible = ['budget', 'cashflow', 'asset', 'transfer', 'liability', 'investment']
    if (incompatible.includes(reportRole) && !(code ?? '').startsWith('INC-04')) {
      reportRole = inferDefaultReportRole(parentId, code, txnTypes)
      warnings.push('Reporting classification aligned with Income transaction type.')
    }
  } else if (txnTypes === 'expense' && !txnTypes.includes('income')) {
    const incomeRoles = ['income', 'refund']
    const tag = semanticTagFromReportRole(reportRole, parentId, code, txnTypes)
    const incomeTags: SemanticTagId[] = ['real_income', 'investment_income', 'other_income', 'refund_reimbursement']
    if (incomeRoles.includes(reportRole) || incomeTags.includes(tag)) {
      reportRole = inferDefaultReportRole(parentId, code, txnTypes)
      warnings.push('Classification aligned with Expense transaction type.')
    }
  }

  return { txnTypes, reportRole, warnings }
}

/** @deprecated Use catalog API */
export const REPORT_ROLE_OPTIONS: Array<{ value: string; label: string }> = []

export function reportRoleLabel(_role?: string): string {
  return '—'
}
