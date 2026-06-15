# M1 — Enrichment Worker ✅

**Status:** Complete  
**Repo:** https://github.com/Suhono-BranchlessPay/Clover/tree/dev

---

## Problem

Clover webhooks at `POST /api/v1/webhook/clover` only include `objectId` references under `merchants[merchantId].payments` — **no amount**. The enrichment worker fetches payment/order details from the Clover REST API and backfills the BP anchor record.

---

## Deliverables

| Item | Status |
|------|--------|
| `cloverEnrichmentWorker.ts` — main worker | ✅ |
| `cloverApiClient.ts` — GET payment/order | ✅ |
| `normalizer.ts` — `clover_payment_created` payload | ✅ |
| `webhookParser.ts` — parse webhook JSON | ✅ |
| Vitest unit tests (18) | ✅ |
| `.env.example` — `CLOVER_APP_ID`, secrets, token | ✅ |

---

## Run tests

```powershell
cd Clover
npm install
npm test
```

---

## Key rules

- Clover `amount` = **cents** → divide by 100 for BP `amount` (USD)
- Payment API: `GET /v3/merchants/{mId}/payments/{paymentId}?expand=order,employee,cardTransaction`
- BP `event_type`: `clover_payment_created` (webhook path)
- Mirror pattern: `xeroEnrichmentWorker.ts` on BP server — wire worker in BP deployment

---

## Handoff to M2

M2 adds the Clover Android APK (`clover_payment` on-device capture). See `docs/MILESTONE_M2.md`.
