import { describe, expect, it } from 'vitest'
import {
  centsToDollars,
  cloverTimestampToIso,
  CloverApiError,
  fetchPayment,
} from '../src/cloverApiClient.js'
import type { CloverPayment } from '../src/types.js'

describe('cloverApiClient', () => {
  it('converts cents to dollars', () => {
    expect(centsToDollars(1500)).toBe(15)
    expect(centsToDollars(99)).toBe(0.99)
    expect(centsToDollars(0)).toBe(0)
    expect(centsToDollars(undefined)).toBe(0)
  })

  it('converts Clover epoch ms to ISO timestamp', () => {
    const iso = cloverTimestampToIso(1_700_000_000_000)
    expect(iso).toMatch(/^\d{4}-\d{2}-\d{2}T/)
  })

  it('fetchPayment calls Clover REST API with bearer token', async () => {
    const sample: CloverPayment = {
      id: 'PAY123',
      amount: 2500,
      tipAmount: 200,
      taxAmount: 150,
      order: { id: 'ORD456' },
      employee: { id: 'EMP1' },
      createdTime: 1_700_000_000_000,
      cardTransaction: { last4: '4242', type: 'VISA' },
    }

    const fetchImpl = async (url: string, init?: RequestInit) => {
      expect(url).toContain('/v3/merchants/M1/payments/PAY123')
      expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer test-token')
      return new Response(JSON.stringify(sample), { status: 200 })
    }

    const payment = await fetchPayment(
      { accessToken: 'test-token', fetchImpl },
      'M1',
      'PAY123',
    )

    expect(payment.id).toBe('PAY123')
    expect(payment.amount).toBe(2500)
  })

  it('fetchPayment throws CloverApiError on 401', async () => {
    const fetchImpl = async () => new Response('unauthorized', { status: 401 })

    await expect(
      fetchPayment({ accessToken: 'bad', fetchImpl }, 'M1', 'X'),
    ).rejects.toBeInstanceOf(CloverApiError)
  })

  it('defaults to sandbox API base when apiBase omitted', async () => {
    let requestedUrl = ''
    const fetchImpl = async (url: string) => {
      requestedUrl = url
      return new Response(JSON.stringify({ id: 'P1', amount: 100 }), { status: 200 })
    }

    await fetchPayment({ accessToken: 't', fetchImpl }, 'M1', 'P1')
    expect(requestedUrl).toContain('apisandbox.dev.clover.com')
  })
})
