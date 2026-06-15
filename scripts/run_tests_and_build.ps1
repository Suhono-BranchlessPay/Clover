$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$javaHome = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$javaHome\bin;$env:PATH"

if (-not (Test-Path ".\gradlew.bat")) {
    Write-Error "gradlew.bat missing — run bootstrap first"
}

& .\gradlew.bat test
& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nAPK: $root\app\build\outputs\apk\debug\app-debug.apk"
}
