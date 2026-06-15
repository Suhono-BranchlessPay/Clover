import { describe, expect, it } from 'vitest'
import { isVerificationWebhook, parseCloverWebhook } from '../src/webhookParser.js'

describe('webhookParser', () => {
  it('parses payment and order objectIds from webhook body', () => {
    const parsed = parseCloverWebhook({
      merchants: {
        ABC123: {
          payments: [{ objectId: 'PAY1' }, { id: 'PAY2' }],
          orders: [{ objectId: 'ORD1' }],
        },
      },
    })

    expect(parsed).toHaveLength(1)
    expect(parsed[0].merchantId).toBe('ABC123')
    expect(parsed[0].paymentIds).toEqual(['PAY1', 'PAY2'])
    expect(parsed[0].orderIds).toEqual(['ORD1'])
  })

  it('returns empty list for verification webhook', () => {
    expect(isVerificationWebhook({ verificationCode: '12345' })).toBe(true)
    expect(parseCloverWebhook({ verificationCode: '12345' })).toEqual([])
  })

  it('handles empty merchants map', () => {
    expect(parseCloverWebhook({})).toEqual([])
  })
})
