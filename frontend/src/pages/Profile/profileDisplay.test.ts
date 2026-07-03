import { describe, expect, it } from 'vitest'
import {
  profileDimensionScoreTier,
  profileDimensionVisual,
  profileLevelTier,
  profileScoreColor,
} from './profileDisplay'

describe('profileLevelTier', () => {
  it('maps moderate exactly to fair (not weak via substring rate)', () => {
    expect(profileLevelTier('moderate')).toBe('fair')
  })

  it('maps needs_attention to weak', () => {
    expect(profileLevelTier('needs_attention')).toBe('weak')
  })

  it('maps strong to strong', () => {
    expect(profileLevelTier('strong')).toBe('strong')
  })
})

describe('profileDimensionScoreTier', () => {
  it('uses stricter bands for debt_pressure', () => {
    expect(profileDimensionScoreTier('debt_pressure', 85)).toBe('strong')
    expect(profileDimensionScoreTier('debt_pressure', 70)).toBe('fair')
    expect(profileDimensionScoreTier('debt_pressure', 50)).toBe('weak')
  })

  it('uses default bands for savings_discipline', () => {
    expect(profileDimensionScoreTier('savings_discipline', 75)).toBe('strong')
    expect(profileDimensionScoreTier('savings_discipline', 50)).toBe('fair')
    expect(profileDimensionScoreTier('savings_discipline', 30)).toBe('weak')
  })
})

describe('profileDimensionVisual', () => {
  it('returns red for high debt pressure (low score)', () => {
    const v = profileDimensionVisual('debt_pressure', 35, 'needs_attention')
    expect(v.tier).toBe('weak')
    expect(v.color).toBe('#dc2626')
    expect(v.antTagColor).toBe('error')
  })

  it('returns green for low debt pressure (high score)', () => {
    const v = profileDimensionVisual('debt_pressure', 95, 'strong')
    expect(v.tier).toBe('strong')
    expect(v.color).toBe('#16a34a')
  })
})

describe('profileScoreColor', () => {
  it('applies dimension thresholds when id provided', () => {
    expect(profileScoreColor(70, 'debt_pressure')).toBe('#d97706')
    expect(profileScoreColor(70, 'savings_discipline')).toBe('#16a34a')
  })
})
