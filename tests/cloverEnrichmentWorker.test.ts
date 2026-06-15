import { describe, expect, it } from 'vitest'
import {
  applyEnrichmentPatch,
  enrichCloverAnchor,
  enrichFromWebhookMerchant,
  processCloverWebhook,
} from '../src/cloverEnrichmentWorker.js'
import { CLOVER_SANDBOX_API_BASE } from '../src/config.js'
import type { CloverOrder, CloverPayment } from '../src/types.js'

const payment: CloverPayment = {
  id: 'PAY-LIVE',
  amount: 6500,
  tipAmount: 500,
  taxAmount: 400,
  order: { id: 'ORD-LIVE' },
  employee: { id: 'E1' },
  createdTime: 1_718_444_000_000,
  cardTransaction: { last4: '9999', type: 'MASTERCARD' },
}

const order: CloverOrder = {
  id: 'ORD-LIVE',
  total: 6500,
  currency: 'USD',
  createdTime: 1_718_444_000_000,
}

function mockApi() {
  const fetchImpl = async (url: string) => {
    if (url.includes('/payments/')) {
      return new Response(JSON.stringify(payment), { status: 200 })
    }
    if (url.includes('/orders/')) {
      return new Response(JSON.stringify(order), { status: 200 })
    }
    return new Response('not found', { status: 404 })
  }

  return { accessToken: 'token', fetchImpl }
}

describe('cloverEnrichmentWorker', () => {
  it('enriches payment anchor from Clover API', async () => {
    const result = await enrichCloverAnchor(
      { merchantId: 'M1', paymentId: 'PAY-LIVE' },
      { api: mockApi() },
    )

    expect(result.enriched).toBe(true)
    expect(result.payload?.amount).toBe(65)
    expect(result.payload?.metadata.last4).toBe('9999')
    expect(result.patch?.amount).toBe(65)
  })

  it('skips when amount already populated', async () => {
    const result = await enrichCloverAnchor(
      { merchantId: 'M1', existing: { amount: 42 } },
      { api: mockApi() },
    )

    expect(result.enriched).toBe(false)
    expect(result.skipped).toBe('amount_already_set')
  })

  it('enriches order when no payment id', async () => {
    const result = await enrichCloverAnchor(
      { merchantId: 'M1', orderId: 'ORD-LIVE' },
      { api: mockApi() },
    )

    expect(result.enriched).toBe(true)
    expect(result.payload?.amount).toBe(65)
    expect(result.orderId).toBe('ORD-LIVE')
  })

  it('processes webhook merchant refs in batch', async () => {
    const results = await enrichFromWebhookMerchant(
      'M1',
      ['PAY-LIVE'],
      ['ORD-LIVE'],
      { api: mockApi() },
    )

    expect(results).toHaveLength(1)
    expect(results[0].enriched).toBe(true)
  })

  it('applyEnrichmentPatch merges metadata', () => {
    const updated = applyEnrichmentPatch(
      { anchor_id: 'abc', amount: 0, metadata: { erp: 'clover' } },
      { amount: 65, metadata: { last4: '9999', merchant_id: 'M1' } },
    )

    expect(updated.amount).toBe(65)
    expect(updated.metadata?.erp).toBe('clover')
    expect(updated.metadata?.last4).toBe('9999')
  })

  it('processCloverWebhook enriches payment refs (xero-style pipeline)', async () => {
    const { results, verification } = await processCloverWebhook(
      {
        merchants: {
          M1: { payments: [{ objectId: 'PAY-LIVE' }] },
        },
      },
      { api: mockApi() },
    )

    expect(verification).toBe(false)
    expect(results).toHaveLength(1)
    expect(results[0].enriched).toBe(true)
    expect(results[0].payload?.event_type).toBe('clover_payment_created')
    expect(results[0].payload?.amount).toBe(65)
  })

  it('processCloverWebhook handles verification challenge', async () => {
    const { verification, results } = await processCloverWebhook(
      { verificationCode: '12345' },
      { api: mockApi() },
    )
    expect(verification).toBe(true)
    expect(results).toEqual([])
  })

  it('returns error result when Clover API fails', async () => {
    const fetchImpl = async () => new Response('not found', { status: 404 })
    const result = await enrichCloverAnchor(
      { merchantId: 'M1', paymentId: 'MISSING' },
      { api: { accessToken: 't', apiBase: CLOVER_SANDBOX_API_BASE, fetchImpl } },
    )
    expect(result.enriched).toBe(false)
    expect(result.error).toContain('404')
  })
})
