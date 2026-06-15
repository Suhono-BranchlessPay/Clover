import type { CloverApiConfig, CloverOrder, CloverPayment } from './types.js'
import { CLOVER_ECOMMERCE_SANDBOX_BASE, CLOVER_SANDBOX_API_BASE } from './config.js'

const DEFAULT_BASE = CLOVER_SANDBOX_API_BASE

function ecommerceBase(config: CloverApiConfig): string {
  return (config.ecommerceBase ?? CLOVER_ECOMMERCE_SANDBOX_BASE).replace(/\/$/, '')
}

async function cloverGetUrl<T>(
  config: CloverApiConfig,
  url: string,
  label: string,
): Promise<T> {
  const fetchImpl = config.fetchImpl ?? fetch
  const response = await fetchImpl(url, {
    headers: {
      Authorization: `Bearer ${config.accessToken}`,
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    const text = await response.text().catch(() => '')
    throw new CloverApiError(`Clover API ${response.status} for ${label}`, response.status, text)
  }

  return response.json() as Promise<T>
}

export class CloverApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly body?: string,
  ) {
    super(message)
    this.name = 'CloverApiError'
  }
}

function baseUrl(config: CloverApiConfig): string {
  return (config.apiBase ?? DEFAULT_BASE).replace(/\/$/, '')
}

async function cloverGet<T>(config: CloverApiConfig, path: string): Promise<T> {
  return cloverGetUrl<T>(config, `${baseUrl(config)}${path}`, path)
}

/** ECOMM orders (scl-sandbox) — requires ecommerce read permission on token. */
export async function fetchEcommerceOrder(
  config: CloverApiConfig,
  orderId: string,
): Promise<Record<string, unknown>> {
  const path = `/v1/orders/${encodeURIComponent(orderId)}`
  return cloverGetUrl<Record<string, unknown>>(
    config,
    `${ecommerceBase(config)}${path}`,
    path,
  )
}

export async function fetchPayment(
  config: CloverApiConfig,
  merchantId: string,
  paymentId: string,
): Promise<CloverPayment> {
  const expand = 'order,employee,cardTransaction'
  return cloverGet<CloverPayment>(
    config,
    `/v3/merchants/${encodeURIComponent(merchantId)}/payments/${encodeURIComponent(paymentId)}?expand=${expand}`,
  )
}

export async function fetchPaymentWithFallback(
  config: CloverApiConfig,
  merchantId: string,
  paymentId: string,
  orderId?: string,
): Promise<CloverPayment> {
  try {
    return await fetchPayment(config, merchantId, paymentId)
  } catch (restError) {
    if (orderId) {
      try {
        const order = await fetchEcommerceOrder(config, orderId)
        const amount = Number(order.amount ?? order.total ?? 0)
        return {
          id: paymentId,
          amount,
          tipAmount: Number(order.tipAmount ?? 0),
          taxAmount: Number(order.taxAmount ?? 0),
          currency: String(order.currency ?? 'USD'),
          order: { id: orderId },
          createdTime: Number(order.createdTime ?? Date.now()),
          cardTransaction: { type: 'CREDIT_CARD' },
        }
      } catch {
        /* fall through */
      }
    }
    throw restError
  }
}

export async function fetchOrder(
  config: CloverApiConfig,
  merchantId: string,
  orderId: string,
): Promise<CloverOrder> {
  return cloverGet<CloverOrder>(
    config,
    `/v3/merchants/${encodeURIComponent(merchantId)}/orders/${encodeURIComponent(orderId)}?expand=lineItems,payments`,
  )
}

export async function fetchMerchant(
  config: CloverApiConfig,
  merchantId: string,
): Promise<{ id: string; name?: string }> {
  return cloverGet<{ id: string; name?: string }>(
    config,
    `/v3/merchants/${encodeURIComponent(merchantId)}`,
  )
}

export async function listPayments(
  config: CloverApiConfig,
  merchantId: string,
  limit = 5,
): Promise<CloverPayment[]> {
  const body = await cloverGet<{ elements?: CloverPayment[] }>(
    config,
    `/v3/merchants/${encodeURIComponent(merchantId)}/payments?limit=${limit}`,
  )
  return body.elements ?? []
}

export function centsToDollars(cents: number | undefined | null): number {
  if (cents == null || Number.isNaN(cents)) return 0
  return Math.round(cents) / 100
}

export function cloverTimestampToIso(ms: number | undefined): string {
  if (!ms || ms <= 0) {
    return new Date().toISOString()
  }
  return new Date(ms).toISOString()
}
