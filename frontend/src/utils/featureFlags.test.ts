import { describe, expect, it } from 'vitest'
import { defaultFeatureFlags } from '../api/features'
import type { FsMenuItem } from '../config/menuConfig'
import { filterMenuByFeatures } from './featureFlags'

const sampleMenu: FsMenuItem[] = [
  { key: '/profile', label: 'Profile', path: '/profile' },
  {
    key: 'reports',
    label: 'Reports',
    children: [
      { key: '/reports/cashflow', label: 'Cashflow', path: '/reports/cashflow' },
      { key: '/reports/annual-outlook', label: 'Annual Outlook', path: '/reports/annual-outlook' },
      { key: '/reports/cash-risk', label: 'Cash Risk', path: '/reports/cash-risk' },
    ],
  },
]

describe('filterMenuByFeatures', () => {
  it('hides profile when profile flag is off', () => {
    const filtered = filterMenuByFeatures(sampleMenu, { ...defaultFeatureFlags, profile: false })
    expect(filtered.some((i) => i.path === '/profile')).toBe(false)
  })

  it('hides forecast reports but keeps cashflow', () => {
    const filtered = filterMenuByFeatures(sampleMenu, { ...defaultFeatureFlags, forecast: false })
    const reports = filtered.find((i) => i.key === 'reports')
    expect(reports?.children?.map((c) => c.path)).toEqual(['/reports/cashflow'])
  })
})
