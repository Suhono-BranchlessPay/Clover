# M1 — Enrichment Worker ✅

**Status:** Complete (sandbox-ready)  
**Repo:** https://github.com/Suhono-BranchlessPay/Clover/tree/dev

---

## Problem

Clover webhooks at `POST /api/v1/webhook/clover` only include `objectId` references — **no amount**. The enrichment worker fetches payment/order details from Clover REST API and backfills the BP anchor record.

**API base:** `https://apisandbox.dev.clover.com` (default) — **NOT** `api.clover.com` for dev/sandbox.

---

## Deliverables

| Item | Status |
|------|--------|
| `cloverEnrichmentWorker.ts` — `processCloverWebhook()` (xero pattern) | ✅ |
| `config.ts` — load `.env`, sandbox default | ✅ |
| `cloverApiClient.ts` — GET payment/order/merchant | ✅ |
| `normalizer.ts` — `clover_payment_created` payload | ✅ |
| `webhookParser.ts` — parse webhook JSON | ✅ |
| Vitest unit tests (24+) | ✅ |
| Live sandbox smoke (`npm run test:m1:live`) | ✅ |

---

## Run

```powershell
cd Clover
npm install
python scripts/sync_env_from_downloads.py   # .env from Downloads\.env.txt

npm test              # unit tests
npm run test:m1:live  # live sandbox (needs CLOVER_API_TOKEN + MERCHANT_ID)
npm run m1:live       # CLI smoke
```

---

## BP server wiring (mirror `xeroEnrichmentWorker`)

```typescript
import { processCloverWebhook, createCloverEnrichmentWorker } from './cloverEnrichmentWorker.js'

const options = createCloverEnrichmentWorker(process.env)
const { verification, results } = await processCloverWebhook(webhookBody, options)

if (verification) return { ok: true, challenge: true }

for (const result of results) {
  if (result.enriched && result.patch) {
    await backfillAnchorRecord(result.patch) // amount, metadata, currency
  }
}
```

---

## Key rules

- Clover `amount` = **cents** → divide by 100 for BP `amount` (USD)
- Payment API: `GET /v3/merchants/{mId}/payments/{paymentId}?expand=order,employee,cardTransaction`
- BP `event_type`: `clover_payment_created`
- Sandbox merchant: `26V6BKSEAX311` (Bp Audit shield)

---

## Handoff to M2

Android APK on-device capture → `clover_payment`. See `docs/MILESTONE_M2.md`.
