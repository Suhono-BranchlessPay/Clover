# Milestone M4 — Clover App Market ✅

**Status:** CLOSED (assets + release build scripts)  
**Repo:** https://github.com/Suhono-BranchlessPay/Clover/tree/dev  
**Version:** `1.0.0` (versionCode 5)

---

## Deliverables

| Item | Status |
|------|--------|
| Signed release APK script | ✅ `scripts/build_release_apk.ps1` |
| Release keystore script | ✅ `scripts/generate_release_keystore.ps1` |
| App icon 512×512 | ✅ `store/icon-512.png` |
| Screenshots 1280×800 (×3) | ✅ `store/screenshot-0*.png` |
| Feature graphic 1024×500 | ✅ `store/feature-graphic-1024x500.png` |
| Store listing copy (EN) | ✅ `store/LISTING.md` |
| Privacy policy URL in app | ✅ Settings |
| Submission pack | ✅ `docs/CLOVER_APP_MARKET.md` |

---

## Clover App Market requirements

| Requirement | Value |
|-------------|--------|
| Package | `com.branchlesspay.auditshield.clover` |
| Privacy policy | https://branchlesspay.com/privacy |
| Short description | ≤80 chars — `store/LISTING.md` |
| Portal | https://www.clover.com/developers |

---

## Build release APK

```powershell
powershell -ExecutionPolicy Bypass -File scripts\generate_release_keystore.ps1
powershell -ExecutionPolicy Bypass -File scripts\build_release_apk.ps1
```

---

## Project complete (M1–M4)

| Milestone | Scope |
|-----------|--------|
| M1 | Enrichment worker + sandbox API |
| M2 | Android APK + offline queue |
| M3 | History + Verify WebView |
| M4 | App Market assets + signed release |

Ready for Clover App Market submission after hardware smoke test.
