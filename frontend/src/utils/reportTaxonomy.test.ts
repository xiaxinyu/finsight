import { describe, expect, it } from 'vitest'
import {
  buildReportingClassificationFilterOptions,
  reportingClassificationFilterLabel,
  reportingClassificationFilterSelectOptions,
} from './reportTaxonomy'

describe('reportTaxonomy', () => {
  it('builds grouped classification filters from catalog', () => {
    const options = buildReportingClassificationFilterOptions({
      semanticTagGroups: [{ title: 'Expense', tags: ['dining_spending'] }],
      semanticTags: { dining_spending: { id: 'dining_spending', label: 'Dining' } },
    })
    expect(options.some((o) => o.value === 'consumption')).toBe(true)
    expect(options.some((o) => o.value === 'dining_spending' && o.group === 'Expense')).toBe(true)
    const groups = reportingClassificationFilterSelectOptions(options)
    expect(groups.find((g) => g.label === 'Expense')?.options[0].label).toBe('Dining')
  })

  it('resolves quick filter labels', () => {
    expect(reportingClassificationFilterLabel('transfer')).toBe('Transfers')
    expect(reportingClassificationFilterLabel('dining_spending', {
      semanticTags: { dining_spending: { id: 'dining_spending', label: 'Dining' } },
    })).toBe('Dining')
  })
})
