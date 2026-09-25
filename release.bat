@echo off
REM ============================================================
REM  EasyLauncher - signed RELEASE APK build
REM    release.bat          - build signed APK into output\
REM    release.bat install  - build and install to phone (USB debugging)
REM  Signing key: keystore\easylauncher-release.jks + keystore.properties
REM ============================================================
setlocal
cd /d "%~dp0"

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"

if not exist "%~dp0keystore.properties" (
    echo *** keystore.properties not found - release APK cannot be signed ***
    exit /b 1
)

echo === Building EasyLauncher (fossRelease) ===
call "%~dp0gradlew.bat" assembleFossRelease
if errorlevel 1 (
    echo.
    echo *** BUILD FAILED ***
    exit /b 1
)

if not exist output mkdir output
copy /y "app\build\outputs\apk\foss\release\app-foss-release.apk" "output\EasyLauncher-release.apk" >nul
echo.
echo === OK: %CD%\output\EasyLauncher-release.apk ===

if /i "%~1"=="install" (
    echo === Installing to device ===
    "%ADB%" install -r "output\EasyLauncher-release.apk"
)
endlocal
