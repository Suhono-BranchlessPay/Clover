# Run all Clover M1–M4 automated checks

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "=== Sync .env ===" -ForegroundColor Cyan
python scripts/sync_env_from_downloads.py

Write-Host "`n=== M1 TypeScript tests ===" -ForegroundColor Cyan
npm test

Write-Host "`n=== M1 live sandbox ===" -ForegroundColor Cyan
npm run test:m1:live

Write-Host "`n=== M2/M3 Android unit tests + debug APK ===" -ForegroundColor Cyan
.\gradlew.bat test assembleDebug --no-daemon

Write-Host "`n=== M4 store assets ===" -ForegroundColor Cyan
python scripts/generate_store_assets.py

Write-Host "`n=== Optional: sandbox E2E (create Cash sale in Chrome first) ===" -ForegroundColor Yellow
Write-Host "npm run e2e:sandbox"

Write-Host "`nDone." -ForegroundColor Green
