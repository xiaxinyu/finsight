export type RuleRiskKind =
  | 'DUPLICATE_PATTERN'
  | 'CROSS_CATEGORY_CONFLICT'
  | 'BROAD_KEYWORD'
  | 'DIRECTION_MISMATCH'
  | 'ORPHAN_CATEGORY'
  | 'INVALID_PATTERN'
  | 'NO_CATEGORY'

export type RuleRiskEntry = {
  ruleId?: string
  pattern?: string
  categoryId?: string
  priority?: number
  active?: number
  risks?: RuleRiskKind[]
  suggestion?: string
  highRisk?: boolean
  duplicateGroupKey?: string
  duplicatePeerRuleIds?: string[]
  duplicatePeerCategoryIds?: string[]
}

export type RuleRiskRemediationItem = {
  ruleId?: string
  pattern?: string
  categoryId?: string
  priority?: number
  risks?: RuleRiskKind[]
  suggestedAction?: string
}

export type RuleRiskReport = {
  activeRuleCount?: number
  highRiskRuleCount?: number
  duplicatePatternGroupCount?: number
  crossCategoryConflictRuleCount?: number
  broadKeywordRuleCount?: number
  directionMismatchRuleCount?: number
  orphanRuleCount?: number
  invalidPatternRuleCount?: number
  entries?: RuleRiskEntry[]
  duplicateGroups?: Array<{
    normalizedPattern?: string
    ruleCount?: number
    categories?: string[]
    ruleIds?: string[]
  }>
  remediation?: RuleRiskRemediationItem[]
}

export const RISK_LABELS: Record<RuleRiskKind, string> = {
  DUPLICATE_PATTERN: 'Duplicate',
  CROSS_CATEGORY_CONFLICT: 'Cross-category',
  BROAD_KEYWORD: 'Broad keyword',
  DIRECTION_MISMATCH: 'Direction mismatch',
  ORPHAN_CATEGORY: 'Orphan category',
  INVALID_PATTERN: 'Invalid pattern',
  NO_CATEGORY: 'No category',
}

export const RISK_COLORS: Record<RuleRiskKind, string> = {
  DUPLICATE_PATTERN: 'volcano',
  CROSS_CATEGORY_CONFLICT: 'red',
  BROAD_KEYWORD: 'orange',
  DIRECTION_MISMATCH: 'magenta',
  ORPHAN_CATEGORY: 'gold',
  INVALID_PATTERN: 'default',
  NO_CATEGORY: 'default',
}

export function buildRiskEntryMap(entries?: RuleRiskEntry[]): Map<string, RuleRiskEntry> {
  const map = new Map<string, RuleRiskEntry>()
  for (const entry of entries || []) {
    if (entry.ruleId) map.set(String(entry.ruleId), entry)
  }
  return map
}

export function highRiskRuleIds(entries?: RuleRiskEntry[]): Set<string> {
  const ids = new Set<string>()
  for (const entry of entries || []) {
    if (entry.highRisk && entry.ruleId) ids.add(String(entry.ruleId))
  }
  return ids
}

function csvEscape(value: string): string {
  if (/[",\n]/.test(value)) return `"${value.replace(/"/g, '""')}"`
  return value
}

export function remediationToCsv(items: RuleRiskRemediationItem[]): string {
  const header = ['ruleId', 'pattern', 'categoryId', 'priority', 'risks', 'suggestedAction']
  const rows = (items || []).map((item) => [
    item.ruleId ?? '',
    item.pattern ?? '',
    item.categoryId ?? '',
    item.priority == null ? '' : String(item.priority),
    (item.risks || []).join('|'),
    item.suggestedAction ?? '',
  ].map(csvEscape).join(','))
  return [header.join(','), ...rows].join('\n')
}

export function downloadRemediationCsv(items: RuleRiskRemediationItem[], filename = 'rule-risk-remediation.csv') {
  const blob = new Blob([remediationToCsv(items)], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
