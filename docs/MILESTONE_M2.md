# Milestone M2 — Clover Android APK

**Target:** 2–3 days · **Status:** ✅ Core complete (v0.1.0-m2)  
**Repo:** https://github.com/Suhono-BranchlessPay/Clover/tree/dev  
**Package:** `com.branchlesspay.auditshield.clover`

---

## Deliverables

| Item | Status |
|------|--------|
| Payment event model (`PaymentEvent`) | ✅ |
| Clover payment broadcast listener | ✅ |
| Debug payment simulator (emulator) | ✅ |
| `clover_payment` BP payload (USD, cents → dollars) | ✅ |
| SQLite offline queue | ✅ |
| Max retry 3× | ✅ |
| Foreground `BpAuditService` | ✅ |
| Boot auto-start | ✅ |
| Simulate Payment UI button | ✅ |
| Flush queue button | ✅ |
| Unit tests (17+) | ✅ |
| Clover Android SDK stub + `app/libs/README.md` | ✅ |

---

## Architecture

```
Payment (Clover broadcast / debug simulate)
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
  "reference_id": "PAY-XXXXXXXX",
  "amount": 15.0,
  "currency": "USD",
  "vendor": "clover",
  "metadata": {
    "erp": "clover",
    "erp_system": "Clover POS",
    "payment_method": "card",
    "amount_cents": 1500,
    "device_model": "Clover Flex",
    "device_sn": "..."
  }
}
```

> Webhook enrichment (M1) uses `clover_payment_created` with Clover REST metadata (tip, tax, last4, order_id).

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
.\gradlew.bat test assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Clover hardware (real device)

On Clover devices (`com.clover.engine` installed or `Build.MANUFACTURER` contains Clover):

- `CloverPaymentCapture` listens for payment success broadcasts
- Add `clover-android-sdk.aar` for `CloverConnector.onSaleResponse()` (see `app/libs/README.md`)

---

## Pending for M2 sign-off

- [ ] Real sale on Clover Flex / Station hardware
- [ ] Verify URL from live `clover_payment`
- [ ] Clover App Market signed APK (M3+)

---

## Contact

suhono@branchlesspay.com
