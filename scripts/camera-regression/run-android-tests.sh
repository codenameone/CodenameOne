#!/usr/bin/env bash
# Local-only. Start an emulator first and select it with ANDROID_SERIAL.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL to the emulator or device to test}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
GRADLE="${GRADLE:-gradle}"
PROJECT="$ROOT/scripts/camera-regression/android"
"$GRADLE" -p "$PROJECT" assembleDebug assembleDebugAndroidTest
ADB="$ANDROID_HOME/platform-tools/adb"
"$ADB" -s "$ANDROID_SERIAL" install -r "$PROJECT/build/outputs/apk/debug/camera-regression-debug.apk"
"$ADB" -s "$ANDROID_SERIAL" install -r "$PROJECT/build/outputs/apk/androidTest/debug/camera-regression-debug-androidTest.apk"
LOG="$(mktemp "${TMPDIR:-/tmp}/cn1-camera-test.XXXXXX")"
trap 'rm -f "$LOG"' EXIT
"$ADB" -s "$ANDROID_SERIAL" shell am instrument -w \
    com.codename1.camera.regression.test/androidx.test.runner.AndroidJUnitRunner | tee "$LOG"
grep -Eq 'OK \([0-9]+ tests?\)' "$LOG"
