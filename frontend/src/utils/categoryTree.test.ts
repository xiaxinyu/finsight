import { describe, expect, it } from 'vitest'
import {
  buildCategoryTree,
  collectSubtreeCodes,
  collectSubtreeCodesFromTree,
  toCategoryTreeSelect,
} from './categoryTree'
import type { ConsumeCategoryRow } from '../api/admin'

/** Post-dedup income tree: single INC root, legacy INC-* + retained INCOME-02. */
const postDedupIncome: ConsumeCategoryRow[] = [
  { id: 'INC', code: 'INC', name: '收入', level: 1, parentId: '', sortNo: 1 },
  { id: 'INC-01', code: 'INC-01', name: '工资薪金', level: 2, parentId: 'INC', sortNo: 1 },
  { id: 'INC-99', code: 'INC-99', name: '其他收入', level: 2, parentId: 'INC', sortNo: 99 },
  { id: 'INCOME-02', code: 'INCOME-02', name: '副业经营', level: 2, parentId: 'INC', sortNo: 2 },
  { id: 'TRANSPORT', code: 'TRANSPORT', name: '交通与车辆', level: 1, parentId: '', sortNo: 2 },
  { id: 'TRANS-02', code: 'TRANS-02', name: '打车/网约车', level: 2, parentId: 'TRANSPORT', sortNo: 2 },
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
