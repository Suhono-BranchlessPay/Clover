# Clover App Market submission pack

**Package:** `com.branchlesspay.auditshield.clover`  
**App name:** BP Audit Shield  
**Version:** `1.0.0` (versionCode 5)  
**Privacy policy:** https://branchlesspay.com/privacy  
**Support:** suhono@branchlesspay.com  
**Portal:** https://www.clover.com/developers  
**Repo:** https://github.com/Suhono-BranchlessPay/Clover/tree/dev

---

## App identity

| Field | Value |
|-------|--------|
| Package name | `com.branchlesspay.auditshield.clover` |
| Clover App ID | `KG2FQ5QEB4GPW` (developer portal — not in APK) |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 34 |
| Event type (on-device) | `clover_payment` |
| Event type (webhook/M1) | `clover_payment_created` |

---

## Target devices

| Device | Notes |
|--------|--------|
| **A920 Pro** | Primary target — most popular Clover mobile |
| **Flex** series | Portable POS |
| **Mini** series | Countertop |

Payment capture: `ACTION_PAYMENT_PROCESSED` broadcast + `PaymentConnector.onSaleResponse()` (Clover SDK 323).

---

## Store assets

| Asset | Path | Size |
|-------|------|------|
| Icon | `store/icon-512.png` | 512×512 |
| Screenshot 1 — Main | `store/screenshot-01-main.png` | 1280×800 |
| Screenshot 2 — History | `store/screenshot-02-history.png` | 1280×800 |
| Screenshot 3 — Verify | `store/screenshot-03-verify.png` | 1280×800 |
| Feature graphic | `store/feature-graphic-1024x500.png` | 1024×500 |
| Listing copy | `store/LISTING.md` | English |

Regenerate PNGs:

```powershell
pip install pillow
python scripts/generate_store_assets.py
```

---

## Listing copy (from `store/LISTING.md`)

**Short description (≤80 chars):**

> Blockchain audit trail for Clover POS. BranchlessPay + Monad verification.

**Full description:** see `store/LISTING.md` — automatic payment capture, offline queue, transaction history, verify WebView, share URL, foreground service.

---

## Release APK

Build signed release:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\generate_release_keystore.ps1
# Add keystore passwords to local.properties (see local.properties.example)
powershell -ExecutionPolicy Bypass -File scripts\build_release_apk.ps1
```

Or:

```powershell
.\gradlew.bat assembleRelease
```

**Output:** `app/build/outputs/apk/release/app-release.apk`

---

## Permissions declared

| Permission | Purpose |
|------------|---------|
| `INTERNET` | POST anchors to BranchlessPay API |
| `ACCESS_NETWORK_STATE` | Offline queue / online detection |
| `RECEIVE_BOOT_COMPLETED` | Auto-start audit service after reboot |
| `FOREGROUND_SERVICE` | Background payment capture |
| `FOREGROUND_SERVICE_DATA_SYNC` | Data sync foreground service type |
| `POST_NOTIFICATIONS` | Service running notification |
| `GET_ACCOUNTS` | `CloverAccount.getAccount()` for PaymentConnector |

No payment initiation — app only **listens** for completed Register sales.

---

## Sandbox test merchant

| Field | Value |
|-------|--------|
| Merchant ID | `26V6BKSEAX311` |
| Sandbox URL | https://sandbox.dev.clover.com |
| API base | `https://apisandbox.dev.clover.com` |

**Smoke test:** Install APK → Settings → BP license key → Register Cash/Card sale → Transaction History → verify URL `https://branchlesspay.com/verify/...`

---

## Portal submission steps

1. Log in at https://www.clover.com/developers
2. Open app **Bp Audit Shield** (`KG2FQ5QEB4GPW`)
3. **App Market** → Create / update listing
4. Upload `app-release.apk` (signed)
5. Attach icon + 3 screenshots + feature graphic from `store/`
6. Paste short + full description from `store/LISTING.md`
7. Set privacy policy URL: https://branchlesspay.com/privacy
8. Select compatible devices: A920 Pro, Flex, Mini (and related Clover Android terminals)
9. Submit for review

---

## Pre-submit checklist

- [ ] Signed release APK built (`app-release.apk`)
- [ ] Icon 512×512 uploaded
- [ ] 3 screenshots 1280×800 uploaded
- [ ] Feature graphic 1024×500 uploaded
- [ ] Short description ≤80 chars
- [ ] Full description pasted
- [ ] Privacy policy URL in portal + in-app Settings
- [ ] Tested on Clover sandbox merchant `26V6BKSEAX311`
- [ ] Real sale produces `clover_payment` anchor + verify URL
- [ ] `GET_ACCOUNTS` permission justified (Clover SDK connector)

---

## Related docs

| Doc | Purpose |
|-----|---------|
| `docs/MILESTONE_M2.md` | Android APK + payment capture |
| `docs/MILESTONE_M3.md` | History + Verify WebView |
| `docs/MILESTONE_M4.md` | Release build + assets |
| `docs/SDK_SETUP.md` | Clover Android SDK wiring |
| `store/LISTING.md` | Store listing text |
