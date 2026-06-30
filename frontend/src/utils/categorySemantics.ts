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
  | 'groceries_spending'
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
  | 'finance_fee'
  | 'tax_expense'
  | 'tax_refund'
  | 'transfer'
  | 'finance_loan'
  | 'finance_credit_loan'
  | 'finance_installment'
  | 'investment'
  | 'liability'
  | 'asset_adjustment'
  | 'other'

export type TxnTypeFilter = 'income' | 'expense' | 'both' | 'capital'

/** Admin-facing primary transaction kind (maps to comma-separated {@code txn_types} in DB). */
export type CategoryTxnKind =
  | 'expense'
  | 'income'
  | 'mixed'
  | 'transfer'
  | 'finance'
  | 'tax'
  | 'refund'

export const CATEGORY_TXN_KIND_OPTIONS: Array<{
  value: CategoryTxnKind
  label: string
  hint: string
}> = [
  {
    value: 'expense',
    label: 'Expense',
    hint: 'P&L outflows — dining, shopping, bills',
  },
  {
    value: 'income',
    label: 'Income',
    hint: 'P&L inflows — salary, interest, dividends',
  },
  {
    value: 'mixed',
    label: 'Mixed P&L',
    hint: 'Both income and expense subcategories',
  },
  {
    value: 'transfer',
    label: 'Transfer',
    hint: 'Internal account movements — excluded from income and spending',
  },
  {
    value: 'finance',
    label: 'Finance',
    hint: 'Loans, investments, installments — excluded from income and spending',
  },
  {
    value: 'tax',
    label: 'Tax',
    hint: 'Statutory tax payments and refunds',
  },
  {
    value: 'refund',
    label: 'Refund / Reimbursement',
    hint: 'Returns and expense reimbursements',
  },
]

const CAPITAL_TXN_TOKENS = new Set(['invest', 'liability', 'finance'])

/** Parse stored {@code txn_types} into a single admin kind (backwards compatible). */
export function parseCategoryTxnKind(txnTypes?: string): CategoryTxnKind {
  const tokens = (txnTypes ?? '')
    .toLowerCase()
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (!tokens.length) return 'expense'

  const has = (token: string) => tokens.includes(token)
  const hasAny = (...items: string[]) => items.some(has)

  if (has('refund')) return 'refund'
  if (has('tax')) return 'tax'
  // Portfolio income (dividends, interest) — still P&L, not balance-sheet finance
  if (has('income') && has('invest') && !has('expense') && !has('liability') && !has('finance')) {
    return 'income'
  }
  // Expense with transfer semantics (e.g. gift transfer) — still P&L expense
  if (has('expense') && has('transfer') && !has('asset')) {
    return 'expense'
  }
  if (hasAny(...CAPITAL_TXN_TOKENS)) return 'finance'
  if (has('transfer')) return 'transfer'
  if (has('expense') && has('income')) return 'mixed'
  if (has('income')) return 'income'
  if (has('expense')) return 'expense'
  return 'expense'
}

/** Serialize admin kind to persisted {@code txn_types}. */
export function categoryTxnKindToStorage(kind: CategoryTxnKind): string {
  switch (kind) {
    case 'expense':
      return 'expense'
    case 'income':
      return 'income'
    case 'mixed':
      return 'expense,income'
    case 'transfer':
      return 'transfer,asset'
    case 'finance':
      return 'finance,invest,liability'
    case 'tax':
      return 'tax,expense,income'
    case 'refund':
      return 'income,refund'
    default:
      return 'expense'
  }
}

/** Persist txn_types with legacy SQL tokens where needed (e.g. income,invest for portfolio income). */
export function serializeCategoryTxnTypes(
  kind: CategoryTxnKind,
  semanticTag?: SemanticTagId | null,
): string {
  if (kind === 'income' && semanticTag === 'investment_income') {
    return 'income,invest'
  }
  return categoryTxnKindToStorage(kind)
}

export function categoryTxnKindLabel(kind: CategoryTxnKind): string {
  return CATEGORY_TXN_KIND_OPTIONS.find((o) => o.value === kind)?.label ?? kind
}

/** Default kind when creating a child under an L1 root. */
export function defaultCategoryTxnKindForParent(parentId?: string): CategoryTxnKind {
  const parent = (parentId ?? '').trim().toUpperCase()
  if (parent === 'INC' || parent === 'INCOME') return 'income'
  if (parent === 'REIM' || parent === 'REIMB') return 'refund'
  if (parent === 'ASSET') return 'transfer'
  if (parent === 'LIABILITY' || parent === 'INVEST' || parent === 'WEALTH' || parent === 'FP') return 'finance'
  if (parent === 'FIXED') return 'expense'
  return 'expense'
}

/** Infer kind from reporting classification tag (when unambiguous). */
export function inferTxnKindFromSemanticTag(tag: SemanticTagId): CategoryTxnKind | null {
  switch (tag) {
    case 'transfer':
      return 'transfer'
    case 'finance_loan':
    case 'finance_credit_loan':
    case 'finance_installment':
    case 'investment':
    case 'asset_adjustment':
    case 'liability':
      return 'finance'
    case 'tax_expense':
    case 'tax_refund':
      return 'tax'
    case 'refund_reimbursement':
      return 'refund'
    case 'real_income':
    case 'investment_income':
    case 'other_income':
      return 'income'
    default:
      return null
  }
}

/** User-facing semantic tag labels — fallback when catalog API unavailable. */
export const SEMANTIC_TAG_LABELS: Record<SemanticTagId, string> = {
  real_income: 'Earned',
  investment_income: 'Portfolio',
  other_income: 'MiscIncome',
  refund_reimbursement: 'Refund',
  daily_spending: 'General',
  dining_spending: 'Dining',
  shopping_spending: 'Shopping',
  groceries_spending: 'Groceries',
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
  finance_fee: 'Fee',
  tax_expense: 'Tax paid',
  tax_refund: 'Tax refund',
  transfer: 'Transfer',
  finance_loan: 'Loan',
  finance_credit_loan: 'Credit loan',
  finance_installment: 'Installment',
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
      'groceries_spending',
      'shopping_spending',
      'transport_spending',
      'entertainment_spending',
      'education_spending',
      'medical_spending',
      'social_spending',
      'subscription_spending',
      'essential_spending',
      'finance_fee',
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
    title: 'Transfer',
    appliesTo: 'capital',
    tags: ['transfer'],
  },
  {
    title: 'Finance',
    appliesTo: 'capital',
    tags: [
      'finance_loan',
      'finance_credit_loan',
      'finance_installment',
      'investment',
      'asset_adjustment',
    ],
  },
  {
    title: 'Tax',
    appliesTo: 'both',
    tags: ['tax_expense', 'tax_refund'],
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

  const inferredKind = inferTxnKindFromSemanticTag(semanticTag)
  if (inferredKind && !isFixedCategory(parentId, code)) {
    const currentKind = parseCategoryTxnKind(txnTypes)
    const shouldSync = inferredKind === 'finance'
      || inferredKind === 'transfer'
      || inferredKind === 'tax'
      || inferredKind === 'refund'
      || (inferredKind === 'income' && currentKind === 'expense')
      || (inferredKind === 'expense' && currentKind === 'income')
    if (shouldSync && currentKind !== inferredKind) {
      txnTypes = serializeCategoryTxnTypes(inferredKind, semanticTag)
    }
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
  if (code.startsWith('DAILY-03') || code.startsWith('DAILY-04')) return 'groceries_spending'
  if (code.startsWith('SHOP-')) return 'shopping_spending'
  if (code.startsWith('TRANS-') || code.startsWith('TRAVEL-')) return 'transport_spending'
  if (code.startsWith('ENT-')) return 'entertainment_spending'
  if (code.startsWith('EDU-') && code !== 'EDU-01') return 'education_spending'

  if (/超市|食材|粮油|生鲜|菜场/.test(name)) return 'groceries_spending'
  if (/购物|网上|电商|服饰|美妆|母婴|家居|耐用品|日用品|百货/.test(name)) return 'shopping_spending'
  if (/交通|地铁|公交|打车|网约车|滴滴|停车|油费|充电|过路|车辆|机票|火车|租车|代驾|保养|洗车/.test(name)) {
    return 'transport_spending'
  }
  if (/餐饮|外卖|堂食|早餐|咖啡|饭店|吃饭|小吃/.test(name)) return 'dining_spending'
  if (/娱乐|旅行|旅游|酒店|景点|门票|电影|演出|游戏|健身|运动/.test(name)) return 'entertainment_spending'
  if (/培训|书籍|资料|课程/.test(name)) return 'education_spending'
  if (/宠物/.test(name)) return 'daily_spending'
  if (/医疗|医院|药|体检|挂号|牙科|疫苗/.test(name)) return 'medical_spending'
  if (/家政|快递|保洁|维修/.test(name)) return 'daily_spending'

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
  const kind = parseCategoryTxnKind(txnTypes)
  if (kind === 'income' || kind === 'refund') return 'income'
  if (kind === 'expense') return 'expense'
  if (kind === 'mixed' || kind === 'tax') return 'both'
  if (kind === 'transfer' || kind === 'finance') return 'capital'
  return 'both'
}

/** L1 / domain parents where Capital tags are rarely needed on the category itself. */
const EXPENSE_DOMAIN_PARENTS = new Set([
  'LIVING', 'DAILY', 'SHOPPING', 'SHOP', 'TRANSPORT', 'TRAVEL', 'ENT', 'EDU', 'GIFT', 'SOCIAL', 'OTHER', 'FEE', 'FE',
])

const ADVANCED_SEMANTIC_TAGS = new Set<SemanticTagId>([
  'transfer',
  'finance_loan',
  'finance_credit_loan',
  'finance_installment',
  'investment',
  'liability',
  'asset_adjustment',
])

export const LEGACY_SEMANTIC_TAGS = new Set<SemanticTagId>(['liability'])

export function isLegacySemanticTag(tag?: string | null): boolean {
  return Boolean(tag && LEGACY_SEMANTIC_TAGS.has(tag.trim() as SemanticTagId))
}

export function isCapitalSemanticTag(tag?: string | null): boolean {
  return Boolean(tag && ADVANCED_SEMANTIC_TAGS.has(tag.trim() as SemanticTagId))
}

/** Hide Capital row by default for everyday expense categories; advanced users can expand. */
export function shouldHideCapitalRow(
  parentId?: string,
  categoryCode?: string,
  txnTypes?: string,
): boolean {
  const parent = (parentId ?? '').trim().toUpperCase()
  const code = (categoryCode ?? '').trim().toUpperCase()
  const filter = txnTypeFilter(txnTypes)
  const codeRoot = code.includes('-') ? code.split('-')[0] : code

  if (parent === 'REIM' || parent === 'REIMB' || code.startsWith('REIM-')) return true
  if (['ASSET', 'LIABILITY', 'INVEST', 'WEALTH', 'FP'].includes(parent)) return false
  if ((parent === 'INC' || parent === 'INCOME') && filter !== 'expense') return false

  if (parent === 'FIXED' || code.startsWith('FIXED-')) return true
  if (EXPENSE_DOMAIN_PARENTS.has(parent) || EXPENSE_DOMAIN_PARENTS.has(codeRoot)) return true
  if (filter === 'expense') return true
  if (filter === 'both' && (EXPENSE_DOMAIN_PARENTS.has(parent) || EXPENSE_DOMAIN_PARENTS.has(codeRoot))) {
    return true
  }
  return false
}

export type SemanticTagGroupFilterOptions = {
  /** When true, include Capital row even if hidden by default for this category. */
  includeCapital?: boolean
}

export function filterSemanticTagGroups(
  groups: Array<{ title: string; appliesTo?: string; tags: SemanticTagId[] }>,
  txnTypes?: string,
  parentId?: string,
  categoryCode?: string,
  options?: SemanticTagGroupFilterOptions,
) {
  const filter = txnTypeFilter(txnTypes)
  const kind = parseCategoryTxnKind(txnTypes)
  const parent = (parentId ?? '').trim().toUpperCase()
  const code = (categoryCode ?? '').trim().toUpperCase()
  const includeCapital = options?.includeCapital ?? !shouldHideCapitalRow(parentId, categoryCode, txnTypes)

  if (kind === 'tax') {
    return groups.filter((g) => g.title === 'Tax' || g.appliesTo === 'both')
  }
  if (kind === 'finance') {
    return groups.filter((g) => g.appliesTo === 'capital')
  }
  if (kind === 'transfer') {
    return groups.filter((g) => g.title === 'Transfer')
  }
  if (kind === 'refund') {
    return groups.filter((g) => g.appliesTo === 'income')
  }

  if (parent === 'REIM' || parent === 'REIMB' || code.startsWith('REIM-')) {
    return groups.filter((g) => g.appliesTo === 'income')
  }
  if (parent === 'FIXED' || code.startsWith('FIXED-')) {
    return groups.filter((g) => g.title === 'Fixed' || (includeCapital && g.appliesTo === 'capital'))
  }
  if ((parent === 'INC' || parent === 'INCOME') && filter === 'income') {
    return groups.filter((g) => g.appliesTo === 'income')
  }
  if ((parent === 'ASSET' || parent === 'LIABILITY' || parent === 'INVEST' || parent === 'WEALTH')
    && filter === 'capital') {
    return groups.filter((g) => g.appliesTo === 'capital')
  }

  return groups.filter((g) => {
    if (g.appliesTo === 'both') {
      return true
    }
    if (g.appliesTo === 'capital' || g.title === 'Transfer' || g.title === 'Finance' || g.title === 'Capital') {
      return includeCapital
    }
    const applies = g.appliesTo ?? 'both'
    if (filter === 'both') return true
    if (applies === 'both') return true
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
  if (/税/.test(categoryName ?? '') && role === 'income') return 'tax_refund'
  if (/税/.test(categoryName ?? '') && (role === 'budget' || role === 'cashflow')) return 'tax_expense'
  if (role === 'investment') {
    if (parent === 'INC' || parent === 'INCOME' || code.startsWith('INC-04')) return 'investment_income'
    return 'investment'
  }
  if (role === 'liability') {
    if (isFixedCategory(parentId, categoryCode)) {
      return flatFixedTagForKind(inferFixedCostKind(parentId, categoryCode) ?? 'repayment')
    }
    if (code === 'DEBT-01') return 'finance_credit_loan'
    if (code === 'DEBT-04') return 'finance_installment'
    if (code === 'DEBT-02' || code === 'DEBT-03' || code === 'INC-08' || parent === 'LIABILITY') {
      return 'finance_loan'
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
    case 'groceries_spending':
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
    case 'essential_spending':
    case 'finance_fee':
      return 'cashflow'
    case 'tax_expense':
      return 'cashflow'
    case 'tax_refund':
      return 'refund'
    case 'transfer': return 'transfer'
    case 'finance_loan':
    case 'finance_credit_loan':
    case 'finance_installment':
    case 'liability': return 'liability'
    case 'investment': return 'investment'
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
  categoryName?: string,
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
  const semanticTag = resolveSemanticTag(storedSemanticTag, role, parentId, categoryCode, txnTypes, categoryName)
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
  const kind = parseCategoryTxnKind(txnTypes)
  if ((parent === 'INC' || parent === 'INCOME') && kind === 'expense') {
    return 'Parent is an income group but transaction type is Expense — verify the parent category.'
  }
  if (parent === 'FIXED' && kind === 'income') {
    return 'Fixed categories should use Transaction Type Expense.'
  }
  if ((parent === 'WEALTH' || parent === 'INVEST' || parent === 'LIABILITY') && kind === 'expense') {
    return 'Parent is a finance group — use Finance transaction type.'
  }
  if (parent === 'ASSET' && kind === 'finance') {
    return 'Asset categories usually use Transfer transaction type.'
  }
  if ((parent === 'REIM' || parent === 'REIMB') && kind !== 'refund' && kind !== 'income') {
    return 'Reimbursement categories should use Refund / Reimbursement transaction type.'
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

export const SEMANTIC_GROUP_HINTS: Record<string, string> = {
  Fixed: 'Repayment = recurring budget line (Fixed %). Card/loan principal → Finance.',
  Finance: 'Loan / Credit loan / Installment = excluded from income and spending trends.',
  Tax: 'Statutory taxes — tracked separately from daily spending.',
  Expense: 'Subscription lives here only (not under Fixed).',
}

/** Visible tags per group — hides legacy Debt unless already selected. */
export function visibleSemanticTagsForGroup(
  group: { title: string; tags: SemanticTagId[] },
  activeTag?: SemanticTagId,
): SemanticTagId[] {
  if (group.title === 'Finance') {
    return group.tags.filter((t) => !isLegacySemanticTag(t) || activeTag === t)
  }
  return group.tags
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
  } else {
    const kind = parseCategoryTxnKind(txnTypes)
    if (kind === 'income' || kind === 'refund') {
      const incompatible = ['budget', 'cashflow', 'asset', 'transfer', 'liability', 'investment']
      if (incompatible.includes(reportRole) && !(code ?? '').startsWith('INC-04')) {
        reportRole = inferDefaultReportRole(parentId, code, txnTypes)
        warnings.push('Reporting classification aligned with Income transaction type.')
      }
    } else if (kind === 'expense') {
      const incomeRoles = ['income', 'refund']
      const tag = semanticTagFromReportRole(reportRole, parentId, code, txnTypes)
      const incomeTags: SemanticTagId[] = ['real_income', 'investment_income', 'other_income', 'refund_reimbursement']
      if (incomeRoles.includes(reportRole) || incomeTags.includes(tag)) {
        reportRole = inferDefaultReportRole(parentId, code, txnTypes)
        warnings.push('Classification aligned with Expense transaction type.')
      }
    } else if (kind === 'finance') {
      const financeRoles = ['investment', 'liability', 'asset']
      if (!financeRoles.includes(reportRole)) {
        reportRole = inferDefaultReportRole(parentId, code, txnTypes)
        warnings.push('Classification aligned with Finance transaction type.')
      }
    } else if (kind === 'transfer') {
      if (reportRole !== 'transfer' && reportRole !== 'asset') {
        reportRole = 'transfer'
        warnings.push('Classification aligned with Transfer transaction type.')
      }
    } else if (kind === 'tax') {
      const taxRoles = ['cashflow', 'refund']
      if (!taxRoles.includes(reportRole)) {
        reportRole = 'cashflow'
        warnings.push('Classification aligned with Tax transaction type.')
      }
    }
  }

  return { txnTypes, reportRole, warnings }
}

/** @deprecated Use catalog API */
export const REPORT_ROLE_OPTIONS: Array<{ value: string; label: string }> = []

export function reportRoleLabel(_role?: string): string {
  return '—'
}
