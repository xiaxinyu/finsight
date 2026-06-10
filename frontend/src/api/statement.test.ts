import { describe, expect, it } from 'vitest'
import { parseJsonObject } from './normalize'

describe('parseJsonObject', () => {
  it('parses JSON string payloads from CommonResult.data', () => {
    const raw = '{"statementId":"abc-123","rows":120,"parsed":98,"skipped":22}'
    expect(parseJsonObject(raw)).toEqual({
      statementId: 'abc-123',
      rows: 120,
      parsed: 98,
      skipped: 22,
    })
  })

  it('returns object payloads unchanged', () => {
    const obj = { statementId: 'x', rows: 1, parsed: 1, skipped: 0 }
    expect(parseJsonObject(obj)).toEqual(obj)
  })
})
