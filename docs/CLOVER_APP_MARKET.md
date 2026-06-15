# Clover App Market submission

**Package:** `com.branchlesspay.auditshield.clover`  
**Privacy:** https://branchlesspay.com/privacy  
**Portal:** https://www.clover.com/developers

## Assets

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

## Release APK

```powershell
powershell -ExecutionPolicy Bypass -File scripts\generate_release_keystore.ps1
# Add passwords to local.properties
powershell -ExecutionPolicy Bypass -File scripts\build_release_apk.ps1
```

Output: `app/build/outputs/apk/release/app-release.apk`

## Checklist

- [ ] Signed release APK uploaded
- [ ] Icon + 3 screenshots attached
- [ ] Short + full description from `store/LISTING.md`
- [ ] Privacy policy URL in app Settings
- [ ] App tested on Clover sandbox merchant `26V6BKSEAX311`
