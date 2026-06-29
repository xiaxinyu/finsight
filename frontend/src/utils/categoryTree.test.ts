import { describe, expect, it } from 'vitest'
import {
  buildCategoryTree,
  categoryPathLabel,
  categorySpendingDomainLabel,
  collectSubtreeCodes,
  collectSubtreeCodesFromTree,
  toAntTreeNodesWithCounts,
  toCategoryTreeSelect,
} from './categoryTree'
import type { ConsumeCategoryRow } from '../api/admin'

/** Post-dedup income tree: single INC root, legacy INC-* + retained INCOME-02. */
const postDedupIncome: ConsumeCategoryRow[] = [
  { id: 'INC', code: 'INC', name: '收入', level: 1, parentId: '', sortNo: 1, txnTypes: 'income' },
  { id: 'INC-01', code: 'INC-01', name: '工资薪金', level: 2, parentId: 'INC', sortNo: 1, txnTypes: 'income' },
  { id: 'INC-99', code: 'INC-99', name: '其他收入', level: 2, parentId: 'INC', sortNo: 99, txnTypes: 'income' },
  { id: 'INCOME-02', code: 'INCOME-02', name: '副业经营', level: 2, parentId: 'INC', sortNo: 2, txnTypes: 'income' },
  { id: 'TRANSPORT', code: 'TRANSPORT', name: '交通与车辆', level: 1, parentId: '', sortNo: 2, txnTypes: 'expense' },
  { id: 'TRANS-02', code: 'TRANS-02', name: '打车/网约车', level: 2, parentId: 'TRANSPORT', sortNo: 2, txnTypes: 'expense' },
  { id: 'LIVING', code: 'LIVING', name: '日常生活', level: 1, parentId: '', sortNo: 3, txnTypes: 'expense' },
]

describe('buildCategoryTree', () => {
  it('nests L2 under L1 by parent code', () => {
    const tree = buildCategoryTree(postDedupIncome)
    const inc = tree.find((n) => n.code === 'INC')!
    expect(inc.children?.map((c) => c.code)).toEqual(['INC-01', 'INCOME-02', 'INC-99'])
    const transport = tree.find((n) => n.code === 'TRANSPORT')!
    expect(transport.children?.[0].code).toBe('TRANS-02')
  })

  it('nests L2 when parentId references parent id instead of code', () => {
    const rows: ConsumeCategoryRow[] = [
      { id: 'uuid-inc', code: 'INC', name: '收入', parentId: '', sortNo: 1 },
      { id: 'uuid-99', code: 'INC-99', name: '其他收入', parentId: 'uuid-inc', sortNo: 99 },
    ]
    const tree = buildCategoryTree(rows)
    expect(tree).toHaveLength(1)
    expect(tree[0].children?.[0].code).toBe('INC-99')
  })
})

describe('collectSubtreeCodes', () => {
  it('includes root and descendants', () => {
    const codes = collectSubtreeCodes(postDedupIncome, 'INC')
    expect(codes.has('INC')).toBe(true)
    expect(codes.has('INC-99')).toBe(true)
    expect(codes.has('INCOME-02')).toBe(true)
    expect(codes.has('TRANSPORT')).toBe(false)
  })
})

describe('collectSubtreeCodesFromTree', () => {
  it('walks consume-tree nodes', () => {
    const tree = [
      {
        title: '收入',
        value: 'INC',
        children: [
          { title: '工资薪金', value: 'INC-01' },
          { title: '副业经营', value: 'INCOME-02' },
        ],
      },
    ]
    const codes = collectSubtreeCodesFromTree(tree, 'INC')
    expect([...codes]).toEqual(['INC', 'INC-01', 'INCOME-02'])
  })
})

describe('toCategoryTreeSelect', () => {
  const tree = buildCategoryTree(postDedupIncome)

  it('disables excluded codes for merge target picker', () => {
    const nodes = toCategoryTreeSelect(tree, { excludeCodes: new Set(['INCOME-02']) })
    const inc = nodes.find((n) => n.value === 'INC')!
    const side = inc.children!.find((n) => n.value === 'INCOME-02')!
    const salary = inc.children!.find((n) => n.value === 'INC-01')!
    expect(side.disabled).toBe(true)
    expect(salary.disabled).toBeFalsy()
  })

  it('disables L2 when merging L1 into L1', () => {
    const nodes = toCategoryTreeSelect(tree, {
      excludeCodes: new Set(['TRANSPORT']),
      l1TargetsOnly: true,
    })
    const transport = nodes.find((n) => n.value === 'TRANSPORT')!
    expect(transport.disabled).toBe(true)
    expect(nodes.find((n) => n.value === 'INC')!.disabled).toBeFalsy()
  })
})

describe('toAntTreeNodesWithCounts', () => {
  it('rolls up transaction counts on tree titles', () => {
    const tree = buildCategoryTree(postDedupIncome)
    const summary = {
      'INC-01': { transactionCount: 5, activeRuleCount: 1 },
      'INCOME-02': { transactionCount: 3, activeRuleCount: 0 },
      'TRANS-02': { transactionCount: 8, activeRuleCount: 2 },
    }
    const nodes = toAntTreeNodesWithCounts(tree, summary, postDedupIncome)
    const inc = nodes.find((n) => n.key === 'INC')!
    expect(inc.title).toContain('(8)')
    const transport = nodes.find((n) => n.key === 'TRANSPORT')!
    expect(transport.title).toContain('(8)')
  })
})

describe('categorySpendingDomainLabel', () => {
  it('maps L1 codes to spending domain hints', () => {
    expect(categorySpendingDomainLabel('TRANSPORT', 'TRANS-02')).toBe('Transport')
    expect(categorySpendingDomainLabel('LIVING', 'LIVING-01')).toBe('Dining')
    expect(categorySpendingDomainLabel(undefined, 'SHOP-01')).toBe('Shopping')
  })
})

describe('categoryPathLabel', () => {
  it('shows parent and child names', () => {
    const path = categoryPathLabel(postDedupIncome, postDedupIncome.find((c) => c.code === 'TRANS-02'))
    expect(path).toBe('交通与车辆 → 打车/网约车')
  })
})
