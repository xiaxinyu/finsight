import { describe, expect, it } from 'vitest'
import { fromCategorySpend, fromIncomeExpense, fromYearCompare } from './insights'

describe('fromCategorySpend', () => {
  it('warns when no data', () => {
    const bullets = fromCategorySpend([], 0)
    expect(bullets[0].warn).toBe(true)
  })

  it('flags high concentration', () => {
    const bullets = fromCategorySpend([{ key: 'Food', value: 80 }, { key: 'Travel', value: 20 }], 100)
    expect(bullets.some((b) => b.warn && b.text.includes('concentrated'))).toBe(true)
  })
})

describe('fromIncomeExpense', () => {
  it('counts deficit months', () => {
    const bullets = fromIncomeExpense([
      { income: 100, expense: 50, surplus: 50 },
      { income: 80, expense: 90, surplus: -10 },
    ], '2025')
    expect(bullets.some((b) => b.text.includes('deficit'))).toBe(true)
  })
})

describe('fromYearCompare', () => {
  it('flags large year-over-year shift', () => {
    const bullets = fromYearCompare(100, 200, '2024', '2025')
    expect(bullets.some((b) => b.warn && b.text.includes('Large year-over-year'))).toBe(true)
  })
})
