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

  it('parses commit payload with skipped duplicates', () => {
    const raw = '{"statementId":"s1","total":10,"imported":8,"failed":0,"skippedDuplicates":2}'
    expect(parseJsonObject(raw)).toMatchObject({
      statementId: 's1',
      imported: 8,
      skippedDuplicates: 2,
    })
  })
})
