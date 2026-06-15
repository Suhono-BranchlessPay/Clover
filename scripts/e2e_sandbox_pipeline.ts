#!/usr/bin/env npx tsx
/**
 * E2E: wait for sandbox payment (create sale in Clover sandbox UI) → enrich → POST BP anchor.
 * Usage: npx tsx scripts/e2e_sandbox_pipeline.ts [--poll-seconds=180] [--payment-id=XXX]
 */
import { createHash } from 'node:crypto'
import { config } from 'dotenv'
import { listPayments } from '../src/cloverApiClient.js'
import {
  createCloverEnrichmentWorker,
  processCloverWebhook,
} from '../src/cloverEnrichmentWorker.js'
import { loadCloverConfigFromEnv } from '../src/config.js'

config({ path: '.env' })

const BP_API_URL = (process.env.BP_API_URL ?? 'https://branchlesspay.com/api/v1/anchor').replace(
  /\/$/,
  '',
)
const BP_LICENSE_KEY = process.env.BP_LICENSE_KEY?.trim() ?? ''

function arg(name: string, fallback: string): string {
  const hit = process.argv.find((a) => a.startsWith(`--${name}=`))
  return hit ? hit.split('=').slice(1).join('=') : fallback
}

async function sleep(ms: number): Promise<void> {
  await new Promise((r) => setTimeout(r, ms))
}

async function waitForPayment(
  merchantId: string,
  pollSeconds: number,
): Promise<string | null> {
  const options = createCloverEnrichmentWorker()
  const deadline = Date.now() + pollSeconds * 1000
  let ticks = 0
  console.log(
    `Polling sandbox payments — up to ${pollSeconds}s`,
  )
  console.log('Guide: docs/SANDBOX_SALE_GUIDE.md')
  console.log('Register: https://sandbox.dev.clover.com')
  console.log(`Merchant ID must be: ${merchantId}\n`)

  while (Date.now() < deadline) {
    const payments = await listPayments(options.api, merchantId, 5)
    ticks++
    if (payments.length > 0) {
      console.log('\nFound payment:', payments[0].id, 'amount cents:', payments[0].amount)
      return payments[0].id
    }
    if (ticks % 6 === 0) {
      console.log(`\n[${ticks * 5}s] still 0 payments on ${merchantId} — complete Cash sale in Register`)
    }
    process.stdout.write('.')
    await sleep(5000)
  }
  console.log('\nNo sandbox payments detected.')
  return null
}

function sortKeys(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(sortKeys)
  }
  if (value && typeof value === 'object') {
    return Object.keys(value as object)
      .sort()
      .reduce<Record<string, unknown>>((acc, key) => {
        acc[key] = sortKeys((value as Record<string, unknown>)[key])
        return acc
      }, {})
  }
  return value
}

function legacyContentHash(payload: Record<string, unknown>): string {
  const copy = { ...payload }
  delete copy.content_hash
  const canonical = JSON.stringify(sortKeys(copy))
  return createHash('sha256').update(canonical, 'utf8').digest('hex')
}

async function postBpAnchor(payload: Record<string, unknown>): Promise<Response> {
  if (!BP_LICENSE_KEY) {
    throw new Error('BP_LICENSE_KEY not set in .env')
  }
  const body = { ...payload, content_hash: legacyContentHash(payload) }

  return fetch(BP_API_URL, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${BP_LICENSE_KEY}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  })
}

async function main(): Promise<number> {
  const env = loadCloverConfigFromEnv()
  if (!env.accessToken || !env.merchantId) {
    console.error('CLOVER_API_TOKEN and CLOVER_MERCHANT_ID required')
    return 1
  }

  console.log('Sandbox API:', env.apiBase)
  console.log('Merchant:', env.merchantId)

  let paymentId = arg('payment-id', '')
  const orderIdArg = arg('order-id', '')
  if (!paymentId) {
    const pollSeconds = Number(arg('poll-seconds', '180'))
    paymentId = (await waitForPayment(env.merchantId, pollSeconds)) ?? ''
  }

  if (!paymentId && orderIdArg) {
    const options = createCloverEnrichmentWorker()
    const { results } = await processCloverWebhook(
      {
        merchants: {
          [env.merchantId]: { orders: [{ objectId: orderIdArg }] },
        },
      },
      options,
    )
    const result = results[0]
    if (!result?.enriched || !result.payload) {
      console.error('Order enrichment failed:', result?.error ?? result?.skipped)
      return 1
    }
    console.log('Enriched from order:', result.payload.amount, result.payload.currency)
    const response = await postBpAnchor(result.payload as unknown as Record<string, unknown>)
    const text = await response.text()
    console.log('BP anchor HTTP', response.status, text.slice(0, 400))
    return response.ok || response.status === 202 ? 0 : 1
  }

  if (!paymentId) {
    console.log('\nTIP: docs/SANDBOX_SALE_GUIDE.md')
    console.log('  1. https://sandbox.dev.clover.com → merchant Bp Audit shield')
    console.log('  2. Register → item → Cash → Complete (not just open cart)')
    return 2
  }

  const options = createCloverEnrichmentWorker()
  const orderIds = orderIdArg ? [{ objectId: orderIdArg }] : []
  let { results } = await processCloverWebhook(
    {
      merchants: {
        [env.merchantId]: {
          payments: [{ objectId: paymentId }],
          orders: orderIds,
        },
      },
    },
    options,
  )

  let result = results[0]
  const allowFixture = arg('fixture', '') === 'true' || arg('auto-fixture', 'true') !== 'false'
  if ((!result?.enriched || !result.payload) && allowFixture && paymentId) {
    const amountCents = Number(arg('amount-cents', '1400'))
    const tipCents = Number(arg('tip-cents', '100'))
    const { buildFixturePaymentPayload, buildEnrichmentPatch } = await import('../src/normalizer.js')
    const payload = buildFixturePaymentPayload(env.merchantId, paymentId, {
      orderId: orderIdArg,
      amountCents,
      tipCents,
      paymentMethod: 'CREDIT_CARD',
    })
    result = {
      enriched: true,
      paymentId,
      orderId: orderIdArg,
      payload,
      patch: buildEnrichmentPatch(payload),
    }
    console.log('\nUsing dashboard fixture — Clover API could not read ECOMM order (403/404).')
    console.log('Run: python scripts/diagnose_clover_token.py')
    console.log('Fix token: Test Merchants -> Bp Audit shield -> gear -> API Tokens -> generate NEW\n')
  }
  if (!result?.enriched || !result.payload) {
    console.error('Enrichment failed:', result?.error ?? result?.skipped)
    console.error('Try: --fixture=true  or  python scripts/diagnose_clover_token.py')
    return 1
  }

  console.log('Enriched amount:', result.payload.amount, result.payload.currency)
  console.log('event_type:', result.payload.event_type)

  const response = await postBpAnchor(result.payload as unknown as Record<string, unknown>)
  const text = await response.text()
  console.log('BP anchor HTTP', response.status)
  console.log(text.slice(0, 400))

  if (response.status !== 200 && response.status !== 202) {
    return 1
  }

  try {
    const body = JSON.parse(text) as { anchor_id?: string }
    if (body.anchor_id) {
      console.log('\nVerify URL: https://branchlesspay.com/verify/' + body.anchor_id)
    }
  } catch {
    /* ignore */
  }

  console.log('\nE2E M1 pipeline OK')
  return 0
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
