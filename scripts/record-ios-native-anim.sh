#!/usr/bin/env bash
# Records the REAL native iOS animation (the iOS 26 tab-selection lens morph or
# the UISwitch toggle) from the simulator as the motion reference video for the
# fidelity suite. The video lands in the committed
# goldens/<set>-anim/native-<anim>-<appearance>.mov, next to the deterministic
# CN1 morph frame goldens it is compared against (by eye and by extracted
# frames -- see the ffmpeg hint printed at the end).
#
# Like the still references, this runs LOCALLY on a simulator whose OS matches
# the golden set (ios-26-metal -> an iOS 26 runtime) -- never on CI.
#
# Usage: record-ios-native-anim.sh [tabs|switch] [light|dark] [simulator_udid] [seconds]
set -euo pipefail

ANIM="${1:-tabs}"
APPEARANCE="${2:-light}"
UDID="${3:-17853196-A8A7-45F2-8F06-24E8257945E6}"
SECONDS_TO_RECORD="${4:-6}"
BUNDLE_ID="com.codenameone.fidelity.nativeref"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GOLDEN_SET="${CN1SS_FIDELITY_GOLDEN_SET:-ios-26-metal}"
OUT_DIR="$ROOT/scripts/fidelity-app/goldens/${GOLDEN_SET}-anim"
OUT="$OUT_DIR/native-${ANIM}-${APPEARANCE}.mov"

log() { echo "[record-native-anim] $*"; }

# Build + install the NativeRef app (same app as the still references; the
# animate mode is selected via environment at launch).
NATIVEREF_BUILD_ONLY=1 "$ROOT/scripts/build-ios-native-ref.sh" "$UDID"

mkdir -p "$OUT_DIR"
xcrun simctl terminate "$UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
log "Launching $ANIM/$APPEARANCE animation"
SIMCTL_CHILD_NATIVEREF_MODE=animate \
SIMCTL_CHILD_NATIVEREF_ANIM="$ANIM" \
SIMCTL_CHILD_NATIVEREF_APPEARANCE="$APPEARANCE" \
xcrun simctl launch "$UDID" "$BUNDLE_ID" >/dev/null
sleep 2

log "Recording ${SECONDS_TO_RECORD}s -> $OUT"
rm -f "$OUT"
xcrun simctl io "$UDID" recordVideo --codec h264 --force "$OUT" &
REC_PID=$!
sleep "$SECONDS_TO_RECORD"
kill -INT "$REC_PID" 2>/dev/null || true
wait "$REC_PID" 2>/dev/null || true
xcrun simctl terminate "$UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true

if [ -s "$OUT" ]; then
  log "Wrote $(du -h "$OUT" | cut -f1) reference video: $OUT"
  log "Extract comparison frames with e.g.:"
  log "  ffmpeg -i '$OUT' -vf fps=10 '$OUT_DIR/native-${ANIM}-${APPEARANCE}-%02d.png'"
else
  log "FATAL: recording produced no output"; exit 4
fi
