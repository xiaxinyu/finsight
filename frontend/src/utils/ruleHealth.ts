import type { ConsumeCategoryRow, ConsumeRuleRow } from '../api/admin'

export type RuleIssue = 'ok' | 'no_category' | 'orphaned' | 'invalid_pattern'

const ORPHAN_KEY = '__orphaned__'
const NO_CAT_KEY = '__no_category__'
const INVALID_KEY = '__invalid__'

export { ORPHAN_KEY, NO_CAT_KEY, INVALID_KEY }

function categoryKeys(cat: ConsumeCategoryRow): string[] {
  const keys = new Set<string>()
  if (cat.id) keys.add(cat.id)
  if (cat.code) keys.add(cat.code)
  return [...keys]
}

export function classifyRule(
  rule: ConsumeRuleRow,
  activeCategories: ConsumeCategoryRow[],
  allCategories: ConsumeCategoryRow[],
): RuleIssue {
  const pattern = rule.pattern?.trim()
  if (!pattern) return 'invalid_pattern'

  const catId = rule.categoryId?.trim()
  if (!catId) return 'no_category'

  const activeKeys = new Set<string>()
  for (const c of activeCategories) {
    for (const k of categoryKeys(c)) activeKeys.add(k)
  }
  if (activeKeys.has(catId)) return 'ok'

  const allKeys = new Map<string, ConsumeCategoryRow>()
  for (const c of allCategories) {
    for (const k of categoryKeys(c)) allKeys.set(k, c)
  }
  const linked = allKeys.get(catId)
  if (linked && linked.deleted === 1) return 'orphaned'
  return 'orphaned'
}

export function issueLabel(issue: RuleIssue): string {
  switch (issue) {
    case 'no_category': return 'No category'
    case 'orphaned': return 'Orphaned category'
    case 'invalid_pattern': return 'Invalid'
    default: return ''
  }
}

export function filterByTreeKey(
  rules: ConsumeRuleRow[],
  key: string,
  activeCategories: ConsumeCategoryRow[],
  allCategories: ConsumeCategoryRow[],
  matchCategory: (rule: ConsumeRuleRow, cat: ConsumeCategoryRow) => boolean,
): ConsumeRuleRow[] {
  if (key === ORPHAN_KEY) {
    return rules.filter((r) => classifyRule(r, activeCategories, allCategories) === 'orphaned')
  }
  if (key === NO_CAT_KEY) {
    return rules.filter((r) => classifyRule(r, activeCategories, allCategories) === 'no_category')
  }
  if (key === INVALID_KEY) {
    return rules.filter((r) => classifyRule(r, activeCategories, allCategories) === 'invalid_pattern')
  }
  if (key === '__all__') return rules
  const cat = activeCategories.find((c) => c.id === key || c.code === key)
  if (!cat) return []
  return rules.filter((r) => matchCategory(r, cat))
}
