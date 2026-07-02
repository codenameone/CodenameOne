#!/usr/bin/env bash
# Records the REAL native Android animation (Material switch toggle or the tab
# indicator slide) from a local emulator/device as the motion reference video,
# committed under goldens/<set>-anim/. Runs LOCALLY (never on CI), on the same
# emulator profile as the still references (see build-android-native-ref.sh).
#
# Usage: record-android-native-anim.sh [tabs|switch] [light|dark] [adb_serial] [seconds]
set -euo pipefail

ANIM="${1:-switch}"
APPEARANCE="${2:-light}"
SERIAL="${3:-}"
SECONDS_TO_RECORD="${4:-6}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJ="$ROOT/scripts/fidelity-app/android-native-ref"
GOLDEN_SET="${CN1SS_FIDELITY_GOLDEN_SET:-android-m3}"
OUT_DIR="$ROOT/scripts/fidelity-app/goldens/${GOLDEN_SET}-anim"
OUT="$OUT_DIR/native-${ANIM}-${APPEARANCE}.mp4"
PKG="com.codenameone.fidelity.nativeref"

log() { echo "[record-native-anim] $*"; }

ADB=(adb)
if [ -n "$SERIAL" ]; then
  ADB=(adb -s "$SERIAL")
fi
"${ADB[@]}" get-state >/dev/null 2>&1 || { log "No device/emulator online"; exit 3; }

log "Building + installing the native-ref APK"
( cd "$PROJ" && ./gradlew -q assembleDebug )
"${ADB[@]}" install -r "$PROJ/app/build/outputs/apk/debug/app-debug.apk" >/dev/null

mkdir -p "$OUT_DIR"
"${ADB[@]}" shell am force-stop "$PKG" >/dev/null 2>&1 || true
log "Launching $ANIM/$APPEARANCE animation"
"${ADB[@]}" shell am start -n "$PKG/.MainActivity" \
  -e mode animate -e anim "$ANIM" -e appearance "$APPEARANCE" >/dev/null
sleep 2

log "Recording ${SECONDS_TO_RECORD}s -> $OUT"
"${ADB[@]}" shell screenrecord --time-limit "$SECONDS_TO_RECORD" /sdcard/nativeref-anim.mp4
"${ADB[@]}" pull /sdcard/nativeref-anim.mp4 "$OUT" >/dev/null
"${ADB[@]}" shell rm -f /sdcard/nativeref-anim.mp4 >/dev/null 2>&1 || true
"${ADB[@]}" shell am force-stop "$PKG" >/dev/null 2>&1 || true

if [ -s "$OUT" ]; then
  log "Wrote $(du -h "$OUT" | cut -f1) reference video: $OUT"
  log "Extract comparison frames with e.g.:"
  log "  ffmpeg -i '$OUT' -vf fps=10 '$OUT_DIR/native-${ANIM}-${APPEARANCE}-%02d.png'"
else
  log "FATAL: recording produced no output"; exit 4
fi
