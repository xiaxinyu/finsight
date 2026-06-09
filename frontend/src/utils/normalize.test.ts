import { describe, expect, it } from 'vitest'
import { isCollectionResult, normalizeResult, parseJsonArray } from '../api/normalize'

describe('normalizeResult', () => {
  it('handles CommonResult success code', () => {
    const r = normalizeResult<{ id: number }>({ code: 20000, data: { id: 1 }, message: 'ok' })
    expect(r.ok).toBe(true)
    expect(r.data).toEqual({ id: 1 })
  })

  it('handles legacy returnCode success', () => {
    const r = normalizeResult<string>({ returnCode: 'success', returnMessage: 'done' })
    expect(r.ok).toBe(true)
    expect(r.data).toBe('done')
  })

  it('handles raw array', () => {
    const r = normalizeResult<number[]>([1, 2])
    expect(r.ok).toBe(true)
    expect(r.data).toEqual([1, 2])
  })

  it('rejects invalid payload', () => {
    const r = normalizeResult(null)
    expect(r.ok).toBe(false)
  })
})

describe('parseJsonArray', () => {
  it('parses JSON string arrays', () => {
    expect(parseJsonArray('[1,2]')).toEqual([1, 2])
  })

  it('returns empty for invalid JSON', () => {
    expect(parseJsonArray('not-json')).toEqual([])
  })
})

describe('isCollectionResult', () => {
  it('detects collection shape', () => {
    expect(isCollectionResult({ total: 1, rows: [] })).toBe(true)
    expect(isCollectionResult({ data: [] })).toBe(false)
  })
})
