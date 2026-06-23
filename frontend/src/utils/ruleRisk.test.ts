import { describe, expect, it } from 'vitest'
import { buildRiskEntryMap, highRiskRuleIds, remediationToCsv, type RuleRiskEntry } from './ruleRisk'

describe('ruleRisk', () => {
  it('buildRiskEntryMap indexes by rule id', () => {
    const entries: RuleRiskEntry[] = [
      { ruleId: 'r1', highRisk: true, risks: ['BROAD_KEYWORD'] },
      { ruleId: 'r2', highRisk: false, risks: [] },
    ]
    const map = buildRiskEntryMap(entries)
    expect(map.get('r1')?.risks).toEqual(['BROAD_KEYWORD'])
    expect(map.has('r2')).toBe(true)
  })

  it('highRiskRuleIds returns only flagged rules', () => {
    const ids = highRiskRuleIds([
      { ruleId: 'a', highRisk: true },
      { ruleId: 'b', highRisk: false },
      { ruleId: 'c', highRisk: true },
    ])
    expect([...ids].sort()).toEqual(['a', 'c'])
  })

  it('remediationToCsv escapes commas and quotes', () => {
    const csv = remediationToCsv([{
      ruleId: 'r1',
      pattern: '支付',
      categoryId: 'DAILY-01',
      priority: 10,
      risks: ['BROAD_KEYWORD', 'DUPLICATE_PATTERN'],
      suggestedAction: 'Disable duplicate rules, or adjust priority — keep one winner',
    }])
    expect(csv.split('\n')).toHaveLength(2)
    expect(csv).toContain('ruleId,pattern,categoryId,priority,risks,suggestedAction')
    expect(csv).toContain('BROAD_KEYWORD|DUPLICATE_PATTERN')
  })
})
