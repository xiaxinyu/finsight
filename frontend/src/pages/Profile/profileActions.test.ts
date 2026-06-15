import { describe, expect, it } from 'vitest'
import type { ProfileDimension } from '../../api/analytics'
import { profileActionLinks } from './profileActions'

const dim: ProfileDimension = {
  id: 'data_trust',
  score: 60,
  level: 'moderate',
  summary: 'Classify more rows',
  evidence: [],
  actions: [
    { label: 'Review unclassified', type: 'open_transactions', payload: { path: '/transactions?unclassified=1' } },
    { label: 'Rules', type: 'open_rules', payload: { path: '/admin/rules' } },
    { label: 'Broken', type: 'x', payload: {} },
  ],
}

describe('profileActionLinks', () => {
  it('returns only actions with paths', () => {
    expect(profileActionLinks(dim)).toEqual([
      { label: 'Review unclassified', path: '/transactions?unclassified=1', type: 'open_transactions' },
      { label: 'Rules', path: '/admin/rules', type: 'open_rules' },
    ])
  })

  it('returns empty list when dimension is missing', () => {
    expect(profileActionLinks(undefined)).toEqual([])
  })
})
