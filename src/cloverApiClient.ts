import type { CloverApiConfig, CloverOrder, CloverPayment } from './types.js'

const DEFAULT_BASE = 'https://api.clover.com'

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

async function cloverGet<T>(
  config: CloverApiConfig,
  path: string,
): Promise<T> {
  const fetchImpl = config.fetchImpl ?? fetch
  const url = `${baseUrl(config)}${path}`
  const response = await fetchImpl(url, {
    headers: {
      Authorization: `Bearer ${config.accessToken}`,
      Accept: 'application/json',
    },
  })

  if (!response.ok) {
    const text = await response.text().catch(() => '')
    throw new CloverApiError(
      `Clover API ${response.status} for ${path}`,
      response.status,
      text,
    )
  }

  return response.json() as Promise<T>
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
