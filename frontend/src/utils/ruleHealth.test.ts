import { describe, expect, it } from 'vitest'
import type { ConsumeCategoryRow, ConsumeRuleRow } from '../api/admin'
import {
  classifyRule,
  isLegacyArchivedOrphan,
  LEGACY_ORPHAN_MARKERS,
  ORPHAN_KEY,
  filterByTreeKey,
} from './ruleHealth'

const activeCategories: ConsumeCategoryRow[] = [
  { id: 'c1', code: 'DAILY-01', name: 'Daily', deleted: 0 },
]

const allCategories: ConsumeCategoryRow[] = [
  ...activeCategories,
  { id: 'old', code: 'OLD-CODE', name: 'Old', deleted: 1 },
]

function rule(partial: Partial<ConsumeRuleRow>): ConsumeRuleRow {
  return {
    id: 'r1',
    pattern: 'test',
    patternType: 'contains',
    categoryId: 'DAILY-01',
    active: 1,
    ...partial,
  }
}

describe('ruleHealth', () => {
  it('treats active missing category as orphaned', () => {
    expect(classifyRule(rule({ categoryId: 'GONE' }), activeCategories, allCategories)).toBe('orphaned')
  })

  it('treats inactive legacy orphan as archived not orphaned', () => {
    const archived = rule({
      categoryId: 'GONE',
      active: 0,
      remark: `note ${LEGACY_ORPHAN_MARKERS[0]}`,
    })
    expect(isLegacyArchivedOrphan(archived)).toBe(true)
    expect(classifyRule(archived, activeCategories, allCategories)).toBe('legacy_archived')
  })

  it('orphan filter excludes archived legacy rules', () => {
    const rules = [
      rule({ id: 'a', categoryId: 'GONE' }),
      rule({ id: 'b', categoryId: 'GONE', active: 0, remark: LEGACY_ORPHAN_MARKERS[1] }),
    ]
    const filtered = filterByTreeKey(rules, ORPHAN_KEY, activeCategories, allCategories, () => false)
    expect(filtered.map((r) => r.id)).toEqual(['a'])
  })
})
