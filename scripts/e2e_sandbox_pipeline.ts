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
  console.log(
    `Polling sandbox payments (create a Cash sale in Clover sandbox Chrome) — up to ${pollSeconds}s...`,
  )

  while (Date.now() < deadline) {
    const payments = await listPayments(options.api, merchantId, 5)
    if (payments.length > 0) {
      console.log('Found payment:', payments[0].id, 'amount cents:', payments[0].amount)
      return payments[0].id
    }
    await sleep(5000)
    process.stdout.write('.')
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
  if (!paymentId) {
    const pollSeconds = Number(arg('poll-seconds', '180'))
    paymentId = (await waitForPayment(env.merchantId, pollSeconds)) ?? ''
  }

  if (!paymentId) {
    console.log('TIP: In sandbox Chrome → Register / Orders → New Sale → Cash → Complete')
    return 2
  }

  const options = createCloverEnrichmentWorker()
  const { results } = await processCloverWebhook(
    {
      merchants: {
        [env.merchantId]: { payments: [{ objectId: paymentId }] },
      },
    },
    options,
  )

  const result = results[0]
  if (!result?.enriched || !result.payload) {
    console.error('Enrichment failed:', result?.error ?? result?.skipped)
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
