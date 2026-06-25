import { describe, expect, it } from 'vitest'
import {
  detectTransactionSemanticTag,
  semanticInclusionHint,
} from './transactionSemantic'

describe('transactionSemantic', () => {
  it('marks salary as real income', () => {
    const tag = detectTransactionSemanticTag({
      consumeCode: 'INCOME-01',
      consumeName: '工资薪金',
      txnKind: 'income',
    })
    expect(tag).toBe('real_income')
    expect(semanticInclusionHint(tag)).toContain('income trend')
  })

  it('marks reimbursement as excluded from income trend', () => {
    const tag = detectTransactionSemanticTag({
      consumeCode: 'REIM-01',
      consumeName: '公司报销',
      txnKind: 'income',
    })
    expect(tag).toBe('reimbursement')
    expect(semanticInclusionHint(tag)).toContain('Excluded from income')
  })

  it('marks investment outflow as investment not consumption', () => {
    const tag = detectTransactionSemanticTag({
      consumeCode: 'INVEST-01',
      consumeName: '基金申购',
      txnKind: 'expense',
    })
    expect(tag).toBe('investment')
    expect(semanticInclusionHint(tag)).toContain('consumption budget')
  })

  it('marks unclassified rows', () => {
    expect(detectTransactionSemanticTag({ txnKind: 'expense' })).toBe('unclassified')
  })

  it('marks borrowing as liability not income', () => {
    const tag = detectTransactionSemanticTag({
      consumeCode: 'INC-08',
      consumeName: '借款（他人借入）',
      txnKind: 'income',
    })
    expect(tag).toBe('liability')
  })
})
