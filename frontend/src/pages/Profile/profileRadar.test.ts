import { describe, expect, it } from 'vitest'
import type { ProfileDimension } from '../../api/analytics'
import { buildProfileRadarOption, profileUserTypeLabel } from './profileRadar'

const sampleDimensions: ProfileDimension[] = [
  {
    id: 'income_stability',
    score: 72,
    level: 'good',
    summary: 'Stable income',
    evidence: [],
    actions: [],
  },
  {
    id: 'spending_control',
    score: 58,
    level: 'fair',
    summary: 'Room to improve',
    evidence: [],
    actions: [],
  },
]

describe('buildProfileRadarOption', () => {
  it('returns empty radar config when dimensions are missing', () => {
    expect(buildProfileRadarOption(undefined)).toEqual({
      tooltip: {},
      radar: { indicator: [], radius: '62%' },
      series: [{ type: 'radar', data: [] }],
    })
    expect(buildProfileRadarOption([])).toEqual({
      tooltip: {},
      radar: { indicator: [], radius: '62%' },
      series: [{ type: 'radar', data: [] }],
    })
  })

  it('maps dimension scores into radar series with per-dimension colors', () => {
    const option = buildProfileRadarOption(sampleDimensions)
    const series = option.series as {
      type: string
      data: { value: number[]; name: string; itemStyle: { color: unknown } }[]
    }[]

    expect(option.radar).toMatchObject({
      indicator: [
        { name: 'Income stability', max: 100 },
        { name: 'Spending control', max: 100 },
      ],
      radius: '68%',
    })
    expect(series[0].data[0].value).toEqual([72, 58])
    expect(series[0].data[0].name).toBe('Profile')
    expect(typeof series[0].data[0].itemStyle.color).toBe('function')
  })

  it('labels known user types', () => {
    expect(profileUserTypeLabel('disciplined_saver')).toBe('Disciplined saver')
    expect(profileUserTypeLabel('custom_type')).toBe('custom type')
  })
})
