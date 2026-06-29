import { describe, expect, it } from 'vitest'
import {
  buildCategoryClassificationRows,
  formatCategoryClassificationPath,
} from './categoryClassificationReport'
import type { ReportPoint } from '../api/report'

describe('categoryClassificationReport', () => {
  it('formats L1 / L2 classification path', () => {
    expect(formatCategoryClassificationPath('日常生活', '餐饮', 'LIVING', 'DAILY-01')).toBe('日常生活 / 餐饮')
    expect(formatCategoryClassificationPath('日常生活', '日常生活', 'LIVING', 'LIVING')).toBe('日常生活')
  })

  it('builds rows with Expense txn type only', () => {
    const rows = buildCategoryClassificationRows([
      { key: '餐饮', name: '餐饮', code: 'DAILY-01', value: 100, level1Code: 'LIVING', level1Name: '日常生活' },
    ] as ReportPoint[])
    expect(rows[0].classification).toBe('日常生活 / 餐饮')
    expect(rows[0].txnType).toBe('Expense')
    expect(rows[0].sharePct).toBe(100)
  })
})
