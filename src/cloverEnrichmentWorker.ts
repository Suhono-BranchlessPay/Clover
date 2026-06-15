import { CloverApiError, fetchOrder, fetchPaymentWithFallback } from './cloverApiClient.js'
import { createWorkerOptionsFromEnv } from './config.js'
import {
  buildEnrichmentPatch,
  CLOVER_EVENT_ORDER_CREATED,
  CLOVER_EVENT_ORDER_UPDATED,
  CLOVER_EVENT_PAYMENT_CREATED,
  inferEventType,
  needsEnrichment,
  normalizeOrderToBpPayload,
  normalizePaymentToBpPayload,
} from './normalizer.js'
import { isVerificationWebhook, parseCloverWebhook } from './webhookParser.js'
import type {
  CloverApiConfig,
  CloverWebhookBody,
  EnrichmentInput,
  EnrichmentResult,
  StoredAnchorRecord,
} from './types.js'

export interface CloverEnrichmentWorkerOptions {
  api: CloverApiConfig
  preferOrderWhenNoPayment?: boolean
}

export interface ProcessCloverWebhookResult {
  verification: boolean
  results: EnrichmentResult[]
}

export function createCloverEnrichmentWorker(
  env: Record<string, string | undefined> = process.env,
): CloverEnrichmentWorkerOptions {
  return createWorkerOptionsFromEnv(env)
}

/**
 * Main entry — mirror xeroEnrichmentWorker / BP webhook handler wiring.
 * Webhook has no amount → fetch payment/order from Clover REST → BP payload patch.
 */
export async function processCloverWebhook(
  body: CloverWebhookBody,
  options: CloverEnrichmentWorkerOptions,
): Promise<ProcessCloverWebhookResult> {
  if (isVerificationWebhook(body)) {
    return { verification: true, results: [] }
  }

  const refs = parseCloverWebhook(body)
  const results: EnrichmentResult[] = []

  for (const ref of refs) {
    results.push(
      ...(await enrichFromWebhookMerchant(
        ref.merchantId,
        ref.paymentIds,
        ref.orderIds,
        options,
      )),
    )
  }

  return { verification: false, results }
}

export async function enrichCloverAnchor(
  input: EnrichmentInput,
  options: CloverEnrichmentWorkerOptions,
): Promise<EnrichmentResult> {
  const { merchantId, paymentId, orderId, existing } = input
  const eventType = inferEventType(existing, input.eventType ?? CLOVER_EVENT_PAYMENT_CREATED)

  if (existing && !needsEnrichment(existing) && !paymentId && !orderId) {
    return { enriched: false, skipped: 'amount_already_set' }
  }

  try {
    if (paymentId) {
      return await enrichFromPayment(merchantId, paymentId, eventType, options, orderId)
    }

    if (orderId) {
      return await enrichFromOrder(merchantId, orderId, eventType, options)
    }

    return { enriched: false, skipped: 'missing_payment_or_order_id' }
  } catch (err) {
    const message =
      err instanceof CloverApiError
        ? `${err.message}${err.body ? `: ${err.body.slice(0, 120)}` : ''}`
        : err instanceof Error
          ? err.message
          : String(err)
    return { enriched: false, error: message, paymentId, orderId }
  }
}

async function enrichFromPayment(
  merchantId: string,
  paymentId: string,
  eventType: string,
  options: CloverEnrichmentWorkerOptions,
  orderIdHint?: string,
): Promise<EnrichmentResult> {
  const payment = await fetchPaymentWithFallback(
    options.api,
    merchantId,
    paymentId,
    orderIdHint,
  )
  let order

  const linkedOrderId = payment.order?.id
  if (linkedOrderId) {
    try {
      order = await fetchOrder(options.api, merchantId, linkedOrderId)
    } catch {
      order = undefined
    }
  }

  const payload = normalizePaymentToBpPayload(merchantId, payment, {
    eventType,
    order,
  })

  return {
    enriched: true,
    paymentId: payment.id,
    orderId: linkedOrderId,
    payload,
    patch: buildEnrichmentPatch(payload),
  }
}

async function enrichFromOrder(
  merchantId: string,
  orderId: string,
  eventType: string,
  options: CloverEnrichmentWorkerOptions,
): Promise<EnrichmentResult> {
  const order = await fetchOrder(options.api, merchantId, orderId)
  const resolvedEvent =
    eventType === CLOVER_EVENT_PAYMENT_CREATED
      ? CLOVER_EVENT_ORDER_CREATED
      : eventType || CLOVER_EVENT_ORDER_UPDATED

  const payload = normalizeOrderToBpPayload(merchantId, order, resolvedEvent)

  return {
    enriched: true,
    orderId: order.id,
    payload,
    patch: buildEnrichmentPatch(payload),
  }
}

export async function enrichFromWebhookMerchant(
  merchantId: string,
  paymentIds: string[],
  orderIds: string[],
  options: CloverEnrichmentWorkerOptions,
): Promise<EnrichmentResult[]> {
  const results: EnrichmentResult[] = []

  for (const paymentId of paymentIds) {
    results.push(
      await enrichCloverAnchor(
        {
          merchantId,
          paymentId,
          orderId: orderIds[0],
          eventType: CLOVER_EVENT_PAYMENT_CREATED,
        },
        options,
      ),
    )
  }

  if (paymentIds.length === 0 || options.preferOrderWhenNoPayment) {
    for (const orderId of orderIds) {
      results.push(
        await enrichCloverAnchor(
          { merchantId, orderId, eventType: CLOVER_EVENT_ORDER_UPDATED },
          options,
        ),
      )
    }
  }

  return results
}

export function applyEnrichmentPatch(
  record: StoredAnchorRecord,
  patch: Partial<StoredAnchorRecord>,
): StoredAnchorRecord {
  return {
    ...record,
    ...patch,
    metadata: {
      ...(record.metadata ?? {}),
      ...(patch.metadata ?? {}),
    },
  }
}
