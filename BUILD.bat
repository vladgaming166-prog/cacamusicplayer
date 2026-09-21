@echo off
setlocal ENABLEDELAYEDEXPANSION
title CacaMusicPlayer Build
cd /d "%~dp0"

echo ============================================
echo   CacaMusicPlayer - automatic APK build
echo ============================================
echo.

REM --- Java ---
if not defined JAVA_HOME (
  where java >nul 2>&1
  if errorlevel 1 (
    echo ERROR: Java was not found.
    echo Install JDK 17 or newer, then run this file again.
    echo.
    pause
    exit /b 1
  )
)

REM --- Android SDK ---
if not defined ANDROID_HOME (
  if defined ANDROID_SDK_ROOT (
    set "ANDROID_HOME=!ANDROID_SDK_ROOT!"
  )
)
if not defined ANDROID_HOME (
  if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
)
if not defined ANDROID_HOME (
  if exist "%USERPROFILE%\AppData\Local\Android\Sdk" set "ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk"
)
if not defined ANDROID_HOME (
  echo ERROR: Android SDK was not found.
  echo.
  echo Install Android Studio, open this folder once, and let it download the SDK.
  echo Or set ANDROID_HOME to your SDK folder, for example:
  echo   set ANDROID_HOME=C:\Users\YOU\AppData\Local\Android\Sdk
  echo.
  pause
  exit /b 1
)

echo Using Android SDK: %ANDROID_HOME%
echo sdk.dir=%ANDROID_HOME:\=\\%> local.properties

echo.
echo Building CacaMusicPlayer.apk  (this downloads Gradle the first time)
echo.

call "%~dp0gradlew.bat" assembleDebug --no-daemon
if errorlevel 1 (
  echo.
  echo BUILD FAILED.
  echo If this is the first run, check your internet connection and try again.
  echo.
  pause
  exit /b 1
)

set "OUT=%~dp0app\build\outputs\apk\debug\CacaMusicPlayer.apk"
if exist "%OUT%" (
  echo.
  echo ============================================
  echo   SUCCESS
  echo   APK: %OUT%
  echo ============================================
) else (
  echo Build finished but CacaMusicPlayer.apk was not found.
  echo Look inside app\build\outputs\apk\
)
echo.
pause
