# BranchlessPay Audit Shield — Clover POS

Blockchain audit trail for Clover POS (USA #2 POS, 1M+ merchants).

| Item | Value |
|------|-------|
| Android package | `com.branchlesspay.auditshield.clover` |
| Version | **1.0.0** (versionCode 4) |
| API (sandbox) | `https://apisandbox.dev.clover.com` |
| BP webhook | `POST https://branchlesspay.com/api/v1/webhook/clover` |
| GitHub | https://github.com/Suhono-BranchlessPay/Clover/tree/dev |

---

## Milestones M1–M4 ✅

| Milestone | Scope | Status |
|-----------|-------|--------|
| **M1** | Enrichment worker + sandbox API | ✅ |
| **M2** | Android APK + offline queue | ✅ |
| **M3** | History + Verify WebView | ✅ |
| **M4** | App Market assets + signed release | ✅ |

Docs: `docs/MILESTONE_M1.md` … `M4` · `docs/CLOVER_APP_MARKET.md`

---

## Quick start

```powershell
python scripts/sync_env_from_downloads.py
powershell -ExecutionPolicy Bypass -File scripts\run_all_milestones.ps1
```

### Sandbox E2E (Chrome)

1. Open **sandbox.dev.clover.com** → merchant **Bp Audit shield** (`26V6BKSEAX311`)
2. **Register** → New Sale → add item → **Cash** → Complete
3. Run:

```powershell
npm run e2e:sandbox
```

Pipeline: detect payment → `processCloverWebhook` → enrich amount → POST BP anchor → verify URL.

---

## Builds

| Build | Output |
|-------|--------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` |

---

## Contact

suhono@branchlesspay.com
