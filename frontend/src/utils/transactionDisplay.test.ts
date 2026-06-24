import { describe, expect, it } from 'vitest'
import { buildTransactionDisplay, cleanBankDescription, merchantCoreRaw } from './transactionDisplay'

describe('merchantCoreRaw', () => {
  it('extracts payee from channel-prefixed strings', () => {
    expect(merchantCoreRaw('支付宝 - 消费 - Stripe Inc')).toBe('Stripe Inc')
  })
})

describe('cleanBankDescription', () => {
  it('picks merchant from @@ delimited bank strings', () => {
    const raw = '代收付@@44201531000*****7375@@@深***务 (集团) 有限公司@@垃圾处理费'
    expect(cleanBankDescription(raw)).toBe('深***务 (集团) 有限公司')
  })

  it('falls back to core extraction for dash-separated strings', () => {
    expect(cleanBankDescription('财付通-北京百度网讯科技有限公司')).toBe('北京百度网讯科技有限公司')
  })
})

describe('buildTransactionDisplay', () => {
  it('prefers opponent name as title', () => {
    const d = buildTransactionDisplay({
      id: '1',
      opponentName: 'Netflix',
      transactionDesc: 'NETFLIX.COM 883920',
    })
    expect(d.title).toBe('Netflix')
    expect(d.subtitle).toContain('NETFLIX')
  })

  it('cleans description when opponent is empty', () => {
    const d = buildTransactionDisplay({
      id: '2',
      transactionDesc: '代收付@@123@@商户A@@服务费',
    })
    expect(d.title).toBe('商户A')
  })
})
