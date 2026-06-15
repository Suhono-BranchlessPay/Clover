@rem BP Audit Shield — build debug APK (uses Android Studio JBR)
@echo off
setlocal
set ROOT=%~dp0..
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d "%ROOT%"
if not exist gradlew.bat (
  echo ERROR: gradlew.bat missing. Open project in Android Studio once to generate wrapper.
  exit /b 1
)

call gradlew.bat assembleDebug
if errorlevel 1 exit /b 1

echo.
echo APK: app\build\outputs\apk\debug\app-debug.apk
endlocal
