# Milestone M3 — Display + Verify ✅

**Status:** CLOSED (scaffold + unit tests; emulator screenshots from Sunmi scaffold)  
**Repo:** https://github.com/Suhono-BranchlessPay/Clover/tree/dev  
**Version:** `1.0.0`

---

## Deliverables

| Item | Status |
|------|--------|
| Transaction history screen | ✅ `TransactionHistoryActivity` |
| Status badges (Pending / Anchored / Failed) | ✅ |
| Verify URL per anchored transaction | ✅ |
| WebView verify page | ✅ `VerifyActivity` |
| VERIFIED badge overlay | ✅ |
| Share verify URL | ✅ |
| Clover receipt stub | ✅ `CloverReceiptHelper` |
| History persisted on anchor | ✅ `recordAnchored()` |
| Open Verify from main after anchor | ✅ |
| Unit tests | ✅ (Gradle) |

---

## User flow

1. **Simulate Payment** (emulator) or Clover sale → anchor queued/anchored
2. **Transaction History** → badges
3. Tap **Anchored** → **Verify WebView** (`branchlesspay.com/verify/[id]`)
4. **Share Verify URL**

Screenshots: `docs/screenshots/m3-*-emulator.png` · `store/screenshot-*.png`

---

## Sandbox E2E (M1 + M3 verify URL)

```powershell
# Create Cash sale in sandbox Chrome, then:
npm run e2e:sandbox
```

Pipeline: webhook refs → `processCloverWebhook` → POST BP anchor → verify URL.

---

## Next: M4

Signed release APK + Clover App Market pack — `docs/CLOVER_APP_MARKET.md`
