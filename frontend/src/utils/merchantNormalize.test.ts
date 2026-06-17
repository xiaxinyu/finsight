import { describe, expect, it } from 'vitest'
import { normalizeMerchantToken, rowMatchesMerchantToken } from './merchantNormalize'

describe('merchantNormalize', () => {
  it('normalizes payment noise and trailing digits', () => {
    expect(normalizeMerchantToken('Netflix.com 883920')).toBe('netflix')
    expect(normalizeMerchantToken('Uber Trip')).toBe('uber')
  })

  it('matches transaction rows by normalized token', () => {
    expect(rowMatchesMerchantToken('', 'NETFLIX.COM 883920', 'netflix')).toBe(true)
    expect(rowMatchesMerchantToken('Amazon', '', 'netflix')).toBe(false)
  })
})
