import { describe, expect, it } from 'vitest'
import { rollupToLevel1 } from './reportAnalytics'
import type { ReportPoint } from '../api/report'

describe('rollupToLevel1', () => {
  it('preserves level1Code for drill-down', () => {
    const rows: ReportPoint[] = [
      { key: '餐饮', value: 100, level1Code: 'LIVING', level1Name: '日常生活' },
      { key: '超市', value: 50, level1Code: 'LIVING', level1Name: '日常生活' },
      { key: '地铁', value: 30, level1Code: 'TRANSPORT', level1Name: '交通出行' },
    ]
    const rolled = rollupToLevel1(rows)
    expect(rolled).toHaveLength(2)
    const living = rolled.find((r) => r.key === '日常生活')
    expect(living?.value).toBe(150)
    expect(living?.level1Code).toBe('LIVING')
  })
})
