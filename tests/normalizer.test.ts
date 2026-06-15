import { describe, expect, it } from 'vitest'
import {
  buildEnrichmentPatch,
  CLOVER_EVENT_PAYMENT_CREATED,
  needsEnrichment,
  normalizeOrderToBpPayload,
  normalizePaymentToBpPayload,
  paymentMethod,
} from '../src/normalizer.js'
import type { CloverOrder, CloverPayment } from '../src/types.js'

const samplePayment: CloverPayment = {
  id: 'PAY-ABC',
  amount: 1500,
  tipAmount: 100,
  taxAmount: 150,
  order: { id: 'ORD-XYZ' },
  employee: { id: 'EMP-9' },
  createdTime: 1_718_000_000_000,
  cardTransaction: { last4: '1234', type: 'VISA' },
}

describe('normalizer', () => {
  it('normalizes payment to BP payload with dollars not cents', () => {
    const payload = normalizePaymentToBpPayload('MERCH1', samplePayment)

    expect(payload.event_type).toBe(CLOVER_EVENT_PAYMENT_CREATED)
    expect(payload.reference_id).toBe('PAY-ABC')
    expect(payload.amount).toBe(15)
    expect(payload.currency).toBe('USD')
    expect(payload.vendor).toBe('clover')
    expect(payload.merchant_id).toBe('MERCH1')
  })

  it('includes metadata tip, tax, last4, employee', () => {
    const payload = normalizePaymentToBpPayload('MERCH1', samplePayment)

    expect(payload.metadata.erp).toBe('clover')
    expect(payload.metadata.order_id).toBe('ORD-XYZ')
    expect(payload.metadata.tip_amount).toBe(1)
    expect(payload.metadata.tax_amount).toBe(1.5)
    expect(payload.metadata.last4).toBe('1234')
    expect(payload.metadata.employee_id).toBe('EMP-9')
    expect(payload.metadata.amount_cents).toBe(1500)
  })

  it('normalizes order totals from cents', () => {
    const order: CloverOrder = {
      id: 'ORD-100',
      total: 4599,
      currency: 'USD',
      createdTime: 1_718_000_000_000,
    }

    const payload = normalizeOrderToBpPayload('MERCH1', order)
    expect(payload.amount).toBe(45.99)
    expect(payload.reference_id).toBe('ORD-100')
    expect(payload.metadata.clover_order_id).toBe('ORD-100')
  })

  it('detects records needing enrichment', () => {
    expect(needsEnrichment({ amount: 0 })).toBe(true)
    expect(needsEnrichment({ amount: 12.5 })).toBe(false)
    expect(needsEnrichment({ amount: 0, metadata: { amount_cents: 500 } })).toBe(false)
  })

  it('builds enrichment patch from payload', () => {
    const payload = normalizePaymentToBpPayload('MERCH1', samplePayment)
    const patch = buildEnrichmentPatch(payload)
    expect(patch.amount).toBe(15)
    expect(patch.metadata?.last4).toBe('1234')
  })

  it('reads payment method from card transaction', () => {
    expect(paymentMethod(samplePayment)).toBe('VISA')
  })
})
