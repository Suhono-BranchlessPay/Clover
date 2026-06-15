$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$javaHome = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$javaHome\bin;$env:PATH"

$localProps = Join-Path $root "local.properties"
if (-not (Test-Path $localProps)) {
    Write-Error "local.properties missing. Copy from local.properties.example"
}

$props = Get-Content $localProps | Where-Object { $_ -match "release\.keystore\.file=(.+)" }
if (-not $props) {
    Write-Host "Release keystore not configured. Running generate_release_keystore.ps1 ..."
    & (Join-Path $PSScriptRoot "generate_release_keystore.ps1")
}

& .\gradlew.bat test assembleRelease --no-daemon

if ($LASTEXITCODE -eq 0) {
    $apk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
    Write-Host "`nRelease APK: $apk"
    if (Test-Path $apk) {
        $info = Get-Item $apk
        Write-Host ("Size: {0:N0} bytes" -f $info.Length)
    }
}
