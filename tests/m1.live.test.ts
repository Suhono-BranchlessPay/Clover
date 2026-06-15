/**
 * M1 live smoke test — sandbox Clover API + enrichment worker.
 * Run: npm run test:m1:live
 */
import { config } from 'dotenv'
import { describe, expect, it } from 'vitest'
import { fetchMerchant, listPayments } from '../src/cloverApiClient.js'
import {
  createCloverEnrichmentWorker,
  processCloverWebhook,
} from '../src/cloverEnrichmentWorker.js'
import { CLOVER_SANDBOX_API_BASE, loadCloverConfigFromEnv } from '../src/config.js'

config({ path: '.env' })

const envConfig = loadCloverConfigFromEnv()
const hasLiveCreds = Boolean(envConfig.accessToken && envConfig.merchantId)

describe.skipIf(!hasLiveCreds)('M1 live — Clover sandbox', () => {
  const options = createCloverEnrichmentWorker()

  it('uses apisandbox.dev.clover.com (not production)', () => {
    expect(options.api.apiBase).toBe(CLOVER_SANDBOX_API_BASE)
    expect(options.api.apiBase).not.toContain('api.clover.com/v3')
  })

  it('fetchMerchant returns Bp Audit shield sandbox merchant', async () => {
    const merchant = await fetchMerchant(options.api, envConfig.merchantId!)
    expect(merchant.id).toBe(envConfig.merchantId)
    expect(merchant.name).toBeTruthy()
    console.log('merchant:', merchant.id, merchant.name)
  })

  it('processCloverWebhook enriches first sandbox payment when present', async () => {
    const payments = await listPayments(options.api, envConfig.merchantId!, 3)
    if (payments.length === 0) {
      console.log('SKIP enrichment: no sandbox payments yet — create a test sale in Clover sandbox')
      return
    }

    const paymentId = payments[0].id
    const { results } = await processCloverWebhook(
      {
        merchants: {
          [envConfig.merchantId!]: { payments: [{ objectId: paymentId }] },
        },
      },
      options,
    )

    expect(results).toHaveLength(1)
    expect(results[0].enriched).toBe(true)
    expect(results[0].payload?.amount).toBeGreaterThan(0)
    console.log('enriched payment', paymentId, 'amount', results[0].payload?.amount)
  })
})
