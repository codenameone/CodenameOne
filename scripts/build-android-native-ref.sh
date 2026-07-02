#!/usr/bin/env bash
# Builds the standalone Android native-reference app (scripts/fidelity-app/
# android-native-ref), runs it on a local emulator/device, and pulls the real
# Material 3 widget captures into the committed Android fidelity goldens.
#
# The native references are generated LOCALLY here (never by CI): committed
# references are what makes a phased old-look/new-look migration possible --
# each golden set is pinned to the design generation it was captured on, and a
# new Material generation becomes a NEW set captured on the new OS
# (CN1SS_FIDELITY_GOLDEN_SET=android-m3e ...), living beside android-m3 until
# the old look is retired.
#
# CONTRACT: capture on the same emulator profile the fidelity CI uses
# (scripts-fidelity.yml: API 36, x86_64, google_apis, default 160dpi) so the
# CN1-side renders and these references share pixel density and dp metrics.
#
# Usage: build-android-native-ref.sh [adb_serial]
set -euo pipefail

SERIAL="${1:-}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJ="$ROOT/scripts/fidelity-app/android-native-ref"
GOLDEN_SET="${CN1SS_FIDELITY_GOLDEN_SET:-android-m3}"
GOLDENS="$ROOT/scripts/fidelity-app/goldens/$GOLDEN_SET"
PKG="com.codenameone.fidelity.nativeref"

log() { echo "[android-native-ref] $*"; }

ADB=(adb)
if [ -n "$SERIAL" ]; then
  ADB=(adb -s "$SERIAL")
fi
"${ADB[@]}" get-state >/dev/null 2>&1 || { log "No device/emulator online (adb get-state failed)"; exit 3; }

DPI="$("${ADB[@]}" shell wm density | grep -Eo '[0-9]+' | head -n1 || echo '?')"
log "Device density: ${DPI}dpi (the android-m3 set is captured at 160)"
if [ "$DPI" != "160" ]; then
  log "WARNING: capturing at ${DPI}dpi will not match the CI emulator profile (160dpi)."
fi

if [ -x "$PROJ/gradlew" ]; then
  GRADLE="$PROJ/gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE=gradle
else
  log "gradle not found (no wrapper at $PROJ/gradlew and none on PATH)"; exit 3
fi

log "Building the native-ref APK"
( cd "$PROJ" && $GRADLE -q assembleDebug )
APK="$PROJ/app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || { log "APK not produced at $APK"; exit 4; }

log "Installing + launching"
"${ADB[@]}" install -r "$APK" >/dev/null
"${ADB[@]}" shell am force-stop "$PKG" >/dev/null 2>&1 || true

REFS="/sdcard/Android/data/$PKG/files/refs"
# Clear any previous run's output BEFORE launching, or the DONE poll below can
# race the app's own cleanup and pull the stale set.
"${ADB[@]}" shell "rm -rf $REFS" >/dev/null 2>&1 || true
"${ADB[@]}" shell am start -n "$PKG/.MainActivity" >/dev/null
log "Waiting for capture to finish"
for i in $(seq 1 60); do
  if "${ADB[@]}" shell "ls $REFS/DONE" >/dev/null 2>&1; then
    break
  fi
  sleep 2
  if [ "$i" = "60" ]; then log "TIMEOUT waiting for $REFS/DONE"; exit 5; fi
done

mkdir -p "$GOLDENS"
TMP="$(mktemp -d)"
"${ADB[@]}" pull "$REFS" "$TMP" >/dev/null
COUNT=0
for f in "$TMP"/refs/*.png; do
  [ -f "$f" ] || continue
  cp -f "$f" "$GOLDENS/$(basename "$f")"
  COUNT=$((COUNT + 1))
done
log "Committed $COUNT native reference(s) into $GOLDENS"
log "Review the diff, then commit the goldens (and refresh the baseline on the next suite run)."
