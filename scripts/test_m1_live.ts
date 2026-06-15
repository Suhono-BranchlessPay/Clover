#!/usr/bin/env npx tsx
/**
 * M1 CLI — verify sandbox config + optional live enrichment.
 */
import { config } from 'dotenv'
import { fetchMerchant, listPayments } from '../src/cloverApiClient.js'
import {
  createCloverEnrichmentWorker,
  processCloverWebhook,
} from '../src/cloverEnrichmentWorker.js'
import { CLOVER_SANDBOX_API_BASE, loadCloverConfigFromEnv } from '../src/config.js'

config({ path: '.env' })

async function main(): Promise<number> {
  const env = loadCloverConfigFromEnv()
  console.log('API base:', env.apiBase)
  console.log('Merchant:', env.merchantId ?? '(not set)')
  console.log('Token:', env.accessToken ? `set (${env.accessToken.length} chars)` : 'MISSING')

  if (env.apiBase !== CLOVER_SANDBOX_API_BASE) {
    console.warn('WARN: expected sandbox base', CLOVER_SANDBOX_API_BASE)
  }

  if (!env.accessToken || !env.merchantId) {
    console.error('ERROR: CLOVER_API_TOKEN and CLOVER_MERCHANT_ID required')
    return 1
  }

  const options = createCloverEnrichmentWorker()
  const merchant = await fetchMerchant(options.api, env.merchantId)
  console.log('OK merchant:', merchant.id, merchant.name)

  const payments = await listPayments(options.api, env.merchantId, 5)
  console.log('Payments in sandbox:', payments.length)

  if (payments.length === 0) {
    console.log('No payments to enrich — worker ready; create a sandbox sale to test E2E.')
    return 0
  }

  const paymentId = payments[0].id
  const { results } = await processCloverWebhook(
    {
      merchants: {
        [env.merchantId]: { payments: [{ objectId: paymentId }] },
      },
    },
    options,
  )

  const first = results[0]
  if (!first?.enriched || !first.payload) {
    console.error('Enrichment failed:', first?.error ?? first?.skipped)
    return 1
  }

  console.log('OK enriched payment', paymentId)
  console.log('  amount:', first.payload.amount, first.payload.currency)
  console.log('  event_type:', first.payload.event_type)
  return 0
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
