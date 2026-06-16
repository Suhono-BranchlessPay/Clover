# Milestone M2 — Clover Android APK

**Target:** 2–3 days · **Status:** ✅ CLOSED (v1.0.0 · versionCode 5)  
**Repo:** https://github.com/Suhono-BranchlessPay/Clover/tree/dev  
**Package:** `com.branchlesspay.auditshield.clover`

**Target devices:** A920 Pro · Flex series · Mini series

---

## Deliverables

| Item | Status |
|------|--------|
| Payment event model (`PaymentEvent`) | ✅ |
| Clover SDK Maven (`clover-android-sdk:323`) | ✅ |
| `PaymentConnector.onSaleResponse()` listener | ✅ |
| `ACTION_PAYMENT_PROCESSED` broadcast + Payment parcel parse | ✅ |
| Composite capture (broadcast + connector, dedupe) | ✅ |
| Debug payment simulator (emulator) | ✅ |
| `clover_payment` BP payload (USD, cents → dollars) | ✅ |
| SQLite offline queue | ✅ |
| Max retry 3× | ✅ |
| Foreground `BpAuditService` | ✅ |
| Boot auto-start | ✅ |
| Simulate Payment UI button | ✅ |
| Flush queue button | ✅ |
| Unit tests (17+) | ✅ |

---

## Architecture

```
Payment (Clover broadcast / PaymentConnector / debug simulate)
    → CompositePaymentCapture (dedupe by payment ID)
    → BpAuditService
    → AnchorProcessor
        → online: POST /api/v1/anchor
        → offline/fail: SQLite queue
    → flush on reconnect / manual flush
```

## BP payload (M2 on-device)

```json
{
  "event_type": "clover_payment",
  "reference_id": "OS69ARZ44Q8QC",
  "amount": 15.0,
  "currency": "USD",
  "vendor": "clover",
  "metadata": {
    "erp": "clover",
    "erp_system": "Clover POS",
    "payment_method": "VISA",
    "amount_cents": 1500,
    "device_model": "Clover A920 Pro",
    "device_sn": "..."
  }
}
```

> Webhook enrichment (M1) uses `clover_payment_created` with Clover REST metadata (tip, tax, last4, order_id).

---

## Configure Clover App ID (connector)

Add to `local.properties` (not committed):

```properties
clover.app.id=KG2FQ5QEB4GPW
```

Run `python scripts/sync_env_from_downloads.py` to copy from `Downloads\Clover\.env.txt`.

---

## Test on emulator

1. Settings → save BP license key
2. Tap **Simulate Payment**
3. Expect verify URL or queued offline message
4. Tap **Flush Offline Queue** if pending

```powershell
adb shell am startservice -n com.branchlesspay.auditshield.clover/.BpAuditService -a com.branchlesspay.auditshield.clover.SIMULATE_PAYMENT
```

```powershell
.\gradlew.bat test assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

---

## Clover hardware (A920 Pro / Flex / Mini)

On Clover devices (`com.clover.engine` installed):

- `CloverPaymentCapture` listens for `com.clover.intent.action.PAYMENT_PROCESSED`
- Parses `clover.intent.extra.PAYMENT` via Clover SDK
- `CloverSdkPaymentCapture` wires `PaymentConnector.onSaleResponse()`

Install release APK → Settings → BP license key → Register sale → verify URL in History.

---

## Pending for production sign-off

- [ ] Real sale on A920 Pro / Flex / Mini sandbox device
- [ ] Verify URL from live `clover_payment` anchor

---

## Contact

suhono@branchlesspay.com
