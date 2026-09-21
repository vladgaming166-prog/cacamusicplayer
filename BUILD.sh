#!/bin/sh
set -e
cd "$(dirname "$0")"
echo "============================================"
echo "  CacaMusicPlayer - automatic APK build"
echo "============================================"

if [ -z "$JAVA_HOME" ]; then
  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java was not found. Install JDK 17 or newer."
    exit 1
  fi
fi

if [ -z "$ANDROID_HOME" ]; then
  if [ -n "$ANDROID_SDK_ROOT" ]; then
    ANDROID_HOME="$ANDROID_SDK_ROOT"
  elif [ -d "$HOME/android-sdk" ]; then
    ANDROID_HOME="$HOME/android-sdk"
  elif [ -d "$HOME/Android/Sdk" ]; then
    ANDROID_HOME="$HOME/Android/Sdk"
  elif [ -d "/home/ubuntu/android-sdk" ]; then
    ANDROID_HOME="/home/ubuntu/android-sdk"
  fi
fi

if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
  echo "ERROR: Android SDK was not found. Set ANDROID_HOME."
  exit 1
fi

echo "Using Android SDK: $ANDROID_HOME"
# Escape for local.properties (keep as-is on Unix)
printf "sdk.dir=%s\n" "$ANDROID_HOME" > local.properties

chmod +x ./gradlew
./gradlew assembleDebug --no-daemon

OUT="app/build/outputs/apk/debug/CacaMusicPlayer.apk"
if [ -f "$OUT" ]; then
  echo
  echo "SUCCESS"
  echo "APK: $(pwd)/$OUT"
else
  echo "Build finished but CacaMusicPlayer.apk was not found."
  exit 1
fi
