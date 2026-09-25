@echo off
REM ============================================================
REM  EasyLauncher - signed RELEASE APK build
REM    Double-click       - build signed APK, open output folder
REM    release.bat install  - build and install to phone (USB debugging)
REM    release.bat auto     - build only (no explorer, no pause)
REM  Signing key: keystore\easylauncher-release.jks + keystore.properties
REM ============================================================
setlocal
cd /d "%~dp0"
title EasyLauncher - release build

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
set "APK=%~dp0output\EasyLauncher-release.apk"

if not exist "%~dp0keystore.properties" (
    echo *** keystore.properties not found - release APK cannot be signed ***
    goto :fail
)

echo === Building EasyLauncher (fossRelease) ===
call "%~dp0gradlew.bat" assembleFossRelease
if errorlevel 1 goto :fail

if not exist "%~dp0output" mkdir "%~dp0output"
copy /y "%~dp0app\build\outputs\apk\foss\release\app-foss-release.apk" "%APK%" >nul
echo.
echo ============================================================
echo  OK: %APK%
echo ============================================================

if /i "%~1"=="install" (
    echo === Installing to device ===
    "%ADB%" install -r "%APK%"
) else if /i not "%~1"=="auto" (
    explorer /select,"%APK%"
)
goto :end

:fail
echo.
echo ************************************************************
echo  BUILD FAILED - see errors above
echo ************************************************************
set "FAILED=1"

:end
REM Keep the window open when started by double-click
if /i not "%~1"=="auto" echo %CMDCMDLINE% | find /i "%~0" >nul && pause
endlocal & if defined FAILED exit /b 1
