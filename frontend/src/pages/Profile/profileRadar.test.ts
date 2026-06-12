import { describe, expect, it } from 'vitest'
import type { ProfileDimension } from '../../api/analytics'
import { buildProfileRadarOption } from './profileRadar'

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

  it('maps dimension scores into radar series', () => {
    const option = buildProfileRadarOption(sampleDimensions)
    const series = option.series as { type: string; data: { value: number[]; name: string }[] }[]

    expect(option.radar).toEqual({
      indicator: [
        { name: 'Income stability', max: 100 },
        { name: 'Spending control', max: 100 },
      ],
      radius: '62%',
    })
    expect(series[0].data[0]).toEqual({ value: [72, 58], name: 'Profile' })
  })
})
