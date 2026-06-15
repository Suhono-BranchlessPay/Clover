# BranchlessPay Audit Shield — Clover POS

Blockchain audit trail for Clover POS devices (USA #2 POS, 1M+ merchants). Webhook enrichment worker (M1) + on-device Android APK (M2).

| Item | Value |
|------|-------|
| Android package | `com.branchlesspay.auditshield.clover` |
| Android version | **0.1.0-m2** (versionCode 1) |
| minSdk | 21 (Android 5.0+) |
| BP anchor API | `POST https://branchlesspay.com/api/v1/anchor` |
| BP webhook (LIVE) | `POST https://branchlesspay.com/api/v1/webhook/clover` |
| Privacy | https://branchlesspay.com/privacy |
| GitHub | https://github.com/Suhono-BranchlessPay/Clover |
| Branch | **`dev`** |

---

## Milestones

| Milestone | Scope | Status |
|-----------|-------|--------|
| **M1** | Webhook enrichment worker (Clover REST API, amount backfill) | ✅ Complete |
| **M2** | Android APK — payment capture + offline queue | ✅ Core complete |
| **M3+** | History polish, App Market release | Planned |

See `docs/MILESTONE_M1.md` · `docs/MILESTONE_M2.md`

---

## M1 — Enrichment worker (TypeScript)

Clover webhooks have no amount — worker calls Clover API and patches the anchor.

```powershell
cd Clover
npm install
npm test
```

Env vars: copy `.env.example` → `.env` (credentials via WhatsApp — do not commit).

---

## M2 — Android APK

Same architecture as Sunmi: foreground service, SQLite offline queue, boot receiver.

```powershell
# Unit tests + debug APK
.\gradlew.bat test assembleDebug
```

1. Open `Clover/` in Android Studio
2. Copy `local.properties.example` → `local.properties`
3. Settings → BP license key → **Simulate Payment** on emulator
4. On Clover hardware: add `clover-android-sdk.aar` (see `app/libs/README.md`)

| Build | Output |
|-------|--------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |

---

## BP payloads

**Webhook (M1 worker):** `clover_payment_created` — amount from REST, metadata includes tip/tax/last4/order_id.

**On-device (M2 APK):** `clover_payment` — amount in **dollars** (Clover cents ÷ 100), currency USD.

---

## Project structure

```
src/                          M1 TypeScript worker
tests/                        M1 Vitest
app/src/main/java/.../clover/ M2 Android app
├── BpAuditService.kt         Foreground service
├── CloverPaymentCapture.kt   Clover broadcast listener
├── AnchorProcessor.kt        Online anchor + offline queue
└── BpAnchorPayload.kt        clover_payment payloads
```

---

## Rules

1. English only (code, docs, comments)
2. No hardcoded credentials — Settings / `.env.example`
3. Private GitHub · `dev` branch
4. Report per milestone to Bos

---

## Contact

suhono@branchlesspay.com
