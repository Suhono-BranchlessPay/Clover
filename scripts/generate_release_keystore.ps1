$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$javaHome = "C:\Program Files\Android\Android Studio\jbr"
$keytool = Join-Path $javaHome "bin\keytool.exe"
if (-not (Test-Path $keytool)) {
    Write-Error "keytool not found at $keytool"
}

$keystoreDir = Join-Path $root "keystore"
$keystoreFile = Join-Path $keystoreDir "bp-audit-shield-release.jks"
New-Item -ItemType Directory -Force -Path $keystoreDir | Out-Null

if (Test-Path $keystoreFile) {
    Write-Host "Keystore already exists: $keystoreFile"
    exit 0
}

$storePass = if ($env:RELEASE_KEYSTORE_PASSWORD) { $env:RELEASE_KEYSTORE_PASSWORD } else { "bp-sunmi-release-dev" }
$keyPass = if ($env:RELEASE_KEY_PASSWORD) { $env:RELEASE_KEY_PASSWORD } else { $storePass }
$alias = "bp-audit-shield"
$dname = "CN=BranchlessPay Audit Shield, OU=Engineering, O=Branchlesspay Inc, L=Jakarta, ST=Jakarta, C=ID"

& $keytool -genkeypair -v `
    -keystore $keystoreFile `
    -alias $alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $storePass `
    -keypass $keyPass `
    -dname $dname

Write-Host "`nCreated: $keystoreFile"
Write-Host "Add to local.properties (gitignored):"
Write-Host "release.keystore.file=keystore/bp-audit-shield-release.jks"
Write-Host "release.keystore.password=$storePass"
Write-Host "release.key.alias=$alias"
Write-Host "release.key.password=$keyPass"

$localProps = Join-Path $root "local.properties"
if (Test-Path $localProps) {
    $content = Get-Content $localProps -Raw
    if ($content -notmatch "release\.keystore\.file") {
        Add-Content $localProps @"

release.keystore.file=keystore/bp-audit-shield-release.jks
release.keystore.password=$storePass
release.key.alias=$alias
release.key.password=$keyPass
"@
        Write-Host "`nAppended signing config to local.properties"
    }
}
