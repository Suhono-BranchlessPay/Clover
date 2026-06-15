import { centsToDollars, cloverTimestampToIso } from './cloverApiClient.js'
import type {
  BpAnchorMetadata,
  BpAnchorPayload,
  CloverOrder,
  CloverPayment,
  StoredAnchorRecord,
} from './types.js'

export const CLOVER_EVENT_PAYMENT_CREATED = 'clover_payment_created'
export const CLOVER_EVENT_ORDER_CREATED = 'clover_order_created'
export const CLOVER_EVENT_ORDER_UPDATED = 'clover_order_updated'

export function inferEventType(existing?: StoredAnchorRecord, fallback = CLOVER_EVENT_PAYMENT_CREATED): string {
  if (existing?.event_type) return existing.event_type
  return fallback
}

export function paymentMethod(payment: CloverPayment): string | undefined {
  return payment.cardTransaction?.type ?? payment.cardTransaction?.cardType
}

export function buildMetadata(
  merchantId: string,
  payment: CloverPayment,
  order?: CloverOrder,
): BpAnchorMetadata {
  const orderId = payment.order?.id ?? order?.id
  const employeeId = payment.employee?.id ?? order?.employee?.id

  return {
    erp: 'clover',
    erp_system: 'Clover POS',
    merchant_id: merchantId,
    order_id: orderId,
    tip_amount: centsToDollars(payment.tipAmount),
    tax_amount: centsToDollars(payment.taxAmount),
    payment_method: paymentMethod(payment),
    last4: payment.cardTransaction?.last4,
    employee_id: employeeId,
    clover_payment_id: payment.id,
    clover_order_id: orderId,
    amount_cents: payment.amount ?? 0,
  }
}

export function normalizePaymentToBpPayload(
  merchantId: string,
  payment: CloverPayment,
  options?: { eventType?: string; order?: CloverOrder; currency?: string },
): BpAnchorPayload {
  const eventType = options?.eventType ?? CLOVER_EVENT_PAYMENT_CREATED
  const currency = options?.currency ?? payment.currency ?? 'USD'
  const metadata = buildMetadata(merchantId, payment, options?.order)

  return {
    event_type: eventType,
    reference_id: payment.id,
    amount: centsToDollars(payment.amount),
    currency,
    timestamp: cloverTimestampToIso(payment.createdTime ?? payment.modifiedTime),
    vendor: 'clover',
    merchant_id: merchantId,
    metadata,
  }
}

export function normalizeOrderToBpPayload(
  merchantId: string,
  order: CloverOrder,
  eventType: string = CLOVER_EVENT_ORDER_UPDATED,
): BpAnchorPayload {
  const metadata: BpAnchorMetadata = {
    erp: 'clover',
    erp_system: 'Clover POS',
    merchant_id: merchantId,
    clover_order_id: order.id,
    employee_id: order.employee?.id,
    amount_cents: order.total ?? 0,
  }

  return {
    event_type: eventType,
    reference_id: order.id,
    amount: centsToDollars(order.total),
    currency: order.currency ?? 'USD',
    timestamp: cloverTimestampToIso(order.createdTime ?? order.modifiedTime),
    vendor: 'clover',
    merchant_id: merchantId,
    metadata,
  }
}

export function buildEnrichmentPatch(payload: BpAnchorPayload): Partial<StoredAnchorRecord> {
  return {
    amount: payload.amount,
    currency: payload.currency,
    timestamp: payload.timestamp,
    merchant_id: payload.merchant_id,
    reference_id: payload.reference_id,
    event_type: payload.event_type,
    metadata: payload.metadata,
  }
}

export function needsEnrichment(record: StoredAnchorRecord): boolean {
  const amount = record.amount ?? 0
  const metaAmount = Number(record.metadata?.amount_cents ?? 0)
  return amount === 0 && metaAmount === 0
}
