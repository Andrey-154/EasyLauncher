@echo off
REM ============================================================
REM  EasyLauncher - debug APK build
REM    build.bat          - build APK into output\
REM    build.bat install  - build and install to phone (USB debugging)
REM ============================================================
setlocal
cd /d "%~dp0"

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"

echo === Building EasyLauncher (fossDebug) ===
call "%~dp0gradlew.bat" assembleFossDebug
if errorlevel 1 (
    echo.
    echo *** BUILD FAILED ***
    exit /b 1
)

if not exist output mkdir output
copy /y "app\build\outputs\apk\foss\debug\app-foss-debug.apk" "output\EasyLauncher-debug.apk" >nul
echo.
echo === OK: %CD%\output\EasyLauncher-debug.apk ===

if /i "%~1"=="install" (
    echo === Installing to device ===
    "%ADB%" install -r -d "output\EasyLauncher-debug.apk"
)
endlocal
