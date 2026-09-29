#!/usr/bin/env bash
# Measures the native iOS tab bar: the Liquid Glass selection MOTION (per-frame
# layer geometry of genuine tap-driven selections) and the bar's MATERIAL
# (lossless screenshots over backdrops whose pixels are known).
#
# This is the instrument behind com.codename1.ui.TabGlassMotion,
# com.codename1.ui.TabGlassGesture and GlassRecipe.liquidPill27. It runs LOCALLY on
# a simulator whose OS matches the golden set, never on CI; the tables it produces
# are committed, and TabGlassMotionTest / TabGlassGestureTest hold the Java models
# to the capture.
#
# Usage: probe-ios-tab-motion.sh [out_dir] [simulator_udid]
#
# Writes into out_dir (default: artifacts/tab-motion-probe):
#   <appearance>-grey-<tabs>tabs.log   per-frame layer logs of tap-driven selections
#   hold-light.log, drag*-<appearance>.log
#                                      per-frame layer logs of held presses and drags
#   live-<appearance>-<backdrop>.png   lossless screenshots of the resting bar
#   snap-<name>/<tap>_<ms>.png         with PROBE_SNAP=1: lossless per-frame snapshots
#                                      of each tap, for side-by-side visual comparison
#                                      (their timing is distorted -- see the README)
# Then regenerate with scripts/fidelity-app/tools/tab-motion/tabmotion.py (see
# scripts/fidelity-app/ios-native-ref/motion-probe/README.md).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$ROOT/artifacts/tab-motion-probe}"
UDID="${2:-}"
GOLDEN_SET="${CN1SS_FIDELITY_GOLDEN_SET:-ios-27-metal}"
PROBE_SRC="$ROOT/scripts/fidelity-app/ios-native-ref/motion-probe"
BUNDLE_ID="com.codenameone.tabprobe"

log() { echo "[probe-tab-motion] $*"; }

# shellcheck source=lib/xcode.sh
source "$ROOT/scripts/lib/xcode.sh"
cn1_select_xcode log || exit 1
log "Using Xcode $CN1_XCODE_VERSION ($DEVELOPER_DIR)"
# shellcheck source=lib/ios-sim.sh
source "$ROOT/scripts/lib/ios-sim.sh"
if [ -z "$UDID" ]; then
    UDID="$(cn1_resolve_ios_sim_udid "$GOLDEN_SET" log)" || exit 1
fi
log "Simulator $UDID ($GOLDEN_SET)"
xcrun simctl boot "$UDID" >/dev/null 2>&1 || true
xcrun simctl bootstatus "$UDID" -b >/dev/null

if ! command -v xcodegen >/dev/null 2>&1; then
    log "xcodegen not on PATH (brew install xcodegen)" >&2
    exit 3
fi
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1-tab-motion-probe-XXXXXX")"
# The spec's paths are relative to the spec, so generate from the source tree and
# only put the generated project and the build products in the temp dir.
xcodegen generate --spec "$PROBE_SRC/project.yml" --project "$WORK" >/dev/null
log "Building probe (xcodebuild build-for-testing)"
xcodebuild build-for-testing -project "$WORK/TabProbe.xcodeproj" -scheme TabProbe \
    -destination "id=$UDID" -derivedDataPath "$WORK/dd" >"$WORK/build.log" 2>&1 \
    || { log "FATAL: probe build failed, see $WORK/build.log"; exit 4; }
mkdir -p "$OUT"

container() { xcrun simctl get_app_container "$UDID" "$BUNDLE_ID" data; }

# One capture: PROBE_SEQ is a comma separated list of gestures, two seconds apart
# (UITests/TapTests.swift): N taps tab N; hN:MS holds tab N for MS ms; dA-B:MS:H
# presses tab A for 0.3 s, drags to tab B over MS ms, holds H ms and releases;
# dA-B.F:MS:H stops at fraction F of the way from A to B.
run_seq() {
    local name="$1" appearance="$2" tabs="$3" seq="$4"
    log "Motion capture $name ($seq)"
    TEST_RUNNER_PROBE_APPEARANCE="$appearance" TEST_RUNNER_PROBE_BACKDROP=grey \
    TEST_RUNNER_PROBE_TABS="$tabs" TEST_RUNNER_PROBE_SEQ="$seq" TEST_RUNNER_PROBE_LOG="$name.log" \
    TEST_RUNNER_PROBE_SNAP="${PROBE_SNAP:-0}" \
    xcodebuild test-without-building -project "$WORK/TabProbe.xcodeproj" -scheme TabProbe \
        -destination "id=$UDID" -derivedDataPath "$WORK/dd" >"$WORK/$name.xcb.txt" 2>&1 \
        || { log "FATAL: capture failed, see $WORK/$name.xcb.txt"; exit 4; }
    cp "$(container)/Documents/$name.log" "$OUT/$name.log"
    if [ "${PROBE_SNAP:-0}" = "1" ]; then
        rm -rf "$OUT/snap-$name"
        cp -R "$(container)/Documents/snap" "$OUT/snap-$name"
    fi
}
# Motion: tap-driven selections. The 3-tab sequence covers one- and two-cell
# jumps both ways; the 5-tab one adds three- and four-cell jumps.
run_taps() { run_seq "$1-grey-$2tabs" "$1" "$2" "$3"; }
run_taps light 3 "2,0,1,2,1,0"
run_taps dark 3 "2,0,1,2,1,0"
run_taps light 5 "4,0,3,1,2,4,3,0"

# Gestures (TabGlassGesture). Holds: two 1.5 s holds and a quick tap on the tab
# that is then selected (nothing travels, so the release wobble is seen alone),
# then a 3 s hold on another tab.
run_seq hold-light light 3 "h1:1500,h1:1500,1,h2:3000"
# Drags: scrubs of every speed, length and hold, both ways, on 3- and 5-tab bars,
# ending on a tab and between tabs, and some that start on an unselected tab.
run_seq drag-light light 3 "d0-1:800:600,d1-2:400:0,d2-0:1200:500,d0-1.5:600:1500,d0-2:250:0"
run_seq dragA-light light 3 "d0-2:1500:300,d2-0:1000:300,d0-2:700:0,d2-0:450:0,d0-2:300:0,d2-0:200:0,d0-2:150:0,d2-0:120:0"
run_seq dragB-light light 3 "d0-1:600:800,d1-2:900:0,d2-1.5:700:1200,d1-0:350:500,d0-2.3:500:900,d2-1:250:0"
run_seq dragC-light light 5 "d0-4:1500:300,d4-0:600:0,d0-4:250:0,d4-1:900:400,d1-3:300:0,d3-2:700:700"
run_seq dragD-dark dark 3 "d0-2:800:300,d2-0:300:0,d0-1:500:600"

# Material: the resting bar, screenshotted losslessly (a screen recording's h264
# smears exactly the detail the blur radius is fitted from).
xcrun simctl install "$UDID" "$(find "$WORK/dd/Build/Products" -name TabProbe.app -maxdepth 2 | head -n1)"
for appearance in light dark; do
    for backdrop in photo stripes grey; do
        xcrun simctl terminate "$UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
        SIMCTL_CHILD_PROBE_APPEARANCE="$appearance" SIMCTL_CHILD_PROBE_BACKDROP="$backdrop" \
            xcrun simctl launch "$UDID" "$BUNDLE_ID" >/dev/null
        sleep 3
        xcrun simctl io "$UDID" screenshot "$OUT/live-$appearance-$backdrop.png" >/dev/null 2>&1
    done
done
xcrun simctl terminate "$UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
log "Wrote $OUT"
