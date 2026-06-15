import type { CloverWebhookBody, CloverWebhookObjectRef } from './types.js'

export interface ParsedWebhookRefs {
  merchantId: string
  paymentIds: string[]
  orderIds: string[]
  verificationCode?: string
}

function objectId(ref: CloverWebhookObjectRef): string | undefined {
  return ref.objectId ?? ref.id
}

export function parseCloverWebhook(body: CloverWebhookBody): ParsedWebhookRefs[] {
  if (body.verificationCode) {
    return []
  }

  const merchants = body.merchants ?? {}
  return Object.entries(merchants).map(([merchantId, payload]) => {
    const paymentIds = (payload.payments ?? [])
      .map(objectId)
      .filter((id): id is string => Boolean(id))
    const orderIds = (payload.orders ?? [])
      .map(objectId)
      .filter((id): id is string => Boolean(id))

    return { merchantId, paymentIds, orderIds }
  })
}

export function isVerificationWebhook(body: CloverWebhookBody): boolean {
  return typeof body.verificationCode === 'string' && body.verificationCode.length > 0
}
