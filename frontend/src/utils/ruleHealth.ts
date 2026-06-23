import type { ConsumeCategoryRow, ConsumeRuleRow } from '../api/admin'

export type RuleIssue = 'ok' | 'no_category' | 'orphaned' | 'invalid_pattern' | 'legacy_archived'

const ORPHAN_KEY = '__orphaned__'
const NO_CAT_KEY = '__no_category__'
const INVALID_KEY = '__invalid__'
const LEGACY_KEY = '__legacy_orphan__'
const HIGH_RISK_KEY = '__high_risk__'

export { ORPHAN_KEY, NO_CAT_KEY, INVALID_KEY, LEGACY_KEY, HIGH_RISK_KEY }

const LEGACY_ORPHAN_REMARK = '[inactive legacy: orphan category]'
const AUTO_DISABLED_ORPHAN_REMARK = '[auto-disabled: orphan category]'
const AUTO_DISABLED_BLANK_PATTERN_REMARK = '[auto-disabled: blank pattern]'
const INACTIVE_LEGACY_BLANK_PATTERN_REMARK = '[inactive legacy: blank pattern]'

export const INVALID_PATTERN_MARKERS = [
  AUTO_DISABLED_BLANK_PATTERN_REMARK,
  INACTIVE_LEGACY_BLANK_PATTERN_REMARK,
]

function categoryKeys(cat: ConsumeCategoryRow): string[] {
  const keys = new Set<string>()
  if (cat.id) keys.add(cat.id)
  if (cat.code) keys.add(cat.code)
  return [...keys]
}

export function isRuleActive(rule: ConsumeRuleRow): boolean {
  return rule.active == null || rule.active !== 0
}

export function isLegacyArchivedOrphan(rule: ConsumeRuleRow): boolean {
  if (isRuleActive(rule)) return false
  const remark = (rule.remark ?? '').toLowerCase()
  return remark.includes('[inactive legacy:') || remark.includes('[auto-disabled: orphan')
}

export function isArchivedInvalidPattern(rule: ConsumeRuleRow): boolean {
  if (isRuleActive(rule)) return false
  const pattern = rule.pattern?.trim()
  if (pattern) return false
  const remark = (rule.remark ?? '').toLowerCase()
  return remark.includes('[auto-disabled: blank pattern]')
    || remark.includes('[inactive legacy: blank pattern]')
}

function pointsToActiveCategory(
  catId: string,
  activeCategories: ConsumeCategoryRow[],
): boolean {
  const activeKeys = new Set<string>()
  for (const c of activeCategories) {
    for (const k of categoryKeys(c)) activeKeys.add(k)
  }
  return activeKeys.has(catId)
}

export function classifyRule(
  rule: ConsumeRuleRow,
  activeCategories: ConsumeCategoryRow[],
  allCategories: ConsumeCategoryRow[],
): RuleIssue {
  const pattern = rule.pattern?.trim()
  if (!pattern) {
    if (isArchivedInvalidPattern(rule)) return 'legacy_archived'
    return 'invalid_pattern'
  }

  const catId = rule.categoryId?.trim()
  if (!catId) return 'no_category'

  if (pointsToActiveCategory(catId, activeCategories)) return 'ok'

  if (isLegacyArchivedOrphan(rule)) return 'legacy_archived'

  if (!isRuleActive(rule)) {
    const allKeys = new Map<string, ConsumeCategoryRow>()
    for (const c of allCategories) {
      for (const k of categoryKeys(c)) allKeys.set(k, c)
    }
    const linked = allKeys.get(catId)
    if (linked && linked.deleted === 1) return 'legacy_archived'
    return 'legacy_archived'
  }

  return 'orphaned'
}

export function issueLabel(issue: RuleIssue): string {
  switch (issue) {
    case 'no_category': return 'No category'
    case 'orphaned': return 'Orphaned category'
    case 'legacy_archived': return 'Inactive legacy'
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
  highRiskRuleIds?: Set<string>,
): ConsumeRuleRow[] {
  if (key === ORPHAN_KEY) {
    return rules.filter((r) => classifyRule(r, activeCategories, allCategories) === 'orphaned')
  }
  if (key === LEGACY_KEY) {
    return rules.filter((r) => classifyRule(r, activeCategories, allCategories) === 'legacy_archived')
  }
  if (key === NO_CAT_KEY) {
    return rules.filter((r) => classifyRule(r, activeCategories, allCategories) === 'no_category')
  }
  if (key === INVALID_KEY) {
    return rules.filter((r) => classifyRule(r, activeCategories, allCategories) === 'invalid_pattern')
  }
  if (key === HIGH_RISK_KEY) {
    if (!highRiskRuleIds?.size) return []
    return rules.filter((r) => r.id && highRiskRuleIds.has(String(r.id)))
  }
  if (key === '__all__') return rules
  const cat = activeCategories.find((c) => c.id === key || c.code === key)
  if (!cat) return []
  return rules.filter((r) => matchCategory(r, cat))
}

export const LEGACY_ORPHAN_MARKERS = [LEGACY_ORPHAN_REMARK, AUTO_DISABLED_ORPHAN_REMARK]
