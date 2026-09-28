#!/usr/bin/env bash
# Records the native iOS 27 tab bar and the Codename One one doing the SAME real
# touches -- taps, a hold, a tap on the selected tab, a slow drag and a flick --
# with the simulator's own screen recorder, and stacks the two side by side.
#
# Native: the motion probe (scripts/fidelity-app/ios-native-ref/motion-probe), a
# real UITabBarController over the shared glass backdrop. Codename One: the
# fidelity app in its live tabs showcase (FidelityDeviceRunner.runTabsShowcase),
# the same Tabs the fidelity goldens use, over the same backdrop. Both are driven
# by the probe's XCUITest bundle, so the touches are genuine on both sides.
#
# Usage: record-ios-tabs-side-by-side.sh <FidelityApp.app> [light|dark|both] [udid] [out_dir]
#
# Writes <out_dir>/tabs-side-by-side-<appearance>.mp4 (full screen, half size) and
# tabs-side-by-side-<appearance>-bar.mp4 (the bottom of the screen at full size),
# native on the left. Needs xcodegen, ffmpeg and a python3 with numpy and Pillow
# (PYTHON=... to choose one). Record with the iOS 27 toolchain, CN1_XCODE_MAJOR=27.
set -euo pipefail

APP="${1:?usage: $0 <FidelityApp.app> [light|dark|both] [udid] [out_dir]}"
WHICH="${2:-both}"
UDID="${3:-}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${4:-$ROOT/artifacts/tabs-side-by-side}"
GOLDEN_SET="${CN1SS_FIDELITY_GOLDEN_SET:-ios-27-metal}"
PROBE_SRC="$ROOT/scripts/fidelity-app/ios-native-ref/motion-probe"
PROBE_ID="com.codenameone.tabprobe"
# Taps both ways, a hold, a tap on the (then) selected tab, a slow drag, a flick.
SEQ="${TABS_SEQ:-2,0,1,2,1,0,h2:1500,2,d2-0:900:300,d0-2:350:0}"
# Tab centres of the Codename One bar in points (x:y per tab); the native side
# reads its own from the accessibility tree.
CN1_POINTS="${CN1_TAB_POINTS:-98.7:782,195.8:782,293.3:782}"
# One gesture per slot. The driver's idle waits differ between the two apps, so
# the gestures are scheduled on a fixed clock and each one is found in its slot.
PERIOD_MS="${TABS_PERIOD_MS:-4500}"

PY="${PYTHON:-python3}"

log() { echo "[tabs-side-by-side] $*"; }

# shellcheck source=lib/xcode.sh
source "$ROOT/scripts/lib/xcode.sh"
cn1_select_xcode log || exit 1
# shellcheck source=lib/ios-sim.sh
source "$ROOT/scripts/lib/ios-sim.sh"
if [ -z "$UDID" ]; then
    UDID="$(cn1_resolve_ios_sim_udid "$GOLDEN_SET" log)" || exit 1
fi
for tool in xcodegen ffmpeg "$PY"; do
    command -v "$tool" >/dev/null 2>&1 || { log "$tool is required" >&2; exit 3; }
done
xcrun simctl boot "$UDID" >/dev/null 2>&1 || true
xcrun simctl bootstatus "$UDID" -b >/dev/null

WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1-tabs-side-by-side-XXXXXX")"
xcodegen generate --spec "$PROBE_SRC/project.yml" --project "$WORK" >/dev/null
log "Building the probe and its XCUITest driver"
xcodebuild build-for-testing -project "$WORK/TabProbe.xcodeproj" -scheme TabProbe \
    -destination "id=$UDID" -derivedDataPath "$WORK/dd" >"$WORK/build.log" 2>&1 \
    || { log "FATAL: probe build failed, see $WORK/build.log"; exit 4; }
CN1_ID="$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$APP/Info.plist")"
xcrun simctl install "$UDID" "$APP"
mkdir -p "$OUT"

# Runs the driver against the probe (no target) or the Codename One app, while the
# simulator records the screen into $1.
record() {
    local video="$1" appearance="$2" target="$3"
    # Wall-clock start of the recording: with the driver log's own wall-clock
    # test start it pins each gesture to its place in the video to within the
    # recorder's start-up latency.
    "$PY" -c 'import time; print("%.3f" % time.time())' > "$video.start"
    xcrun simctl io "$UDID" recordVideo --codec h264 --force "$video" >/dev/null 2>&1 &
    local rec=$!
    sleep 1
    TEST_RUNNER_PROBE_APPEARANCE="$appearance" TEST_RUNNER_PROBE_BACKDROP=photo \
    TEST_RUNNER_PROBE_SEQ="$SEQ" TEST_RUNNER_PROBE_TARGET="$target" \
    TEST_RUNNER_PROBE_TAB_POINTS="$CN1_POINTS" TEST_RUNNER_PROBE_PERIOD_MS="$PERIOD_MS" \
    xcodebuild test-without-building -project "$WORK/TabProbe.xcodeproj" -scheme TabProbe \
        -destination "id=$UDID" -derivedDataPath "$WORK/dd" >"$video.xcb.txt" 2>&1 \
        || { kill -INT "$rec" 2>/dev/null || true; log "FATAL: driver failed, see $video.xcb.txt"; exit 4; }
    # The last gesture's slot must be on screen in full before recording stops.
    sleep 5
    kill -INT "$rec" 2>/dev/null || true
    wait "$rec" 2>/dev/null || true
}

# The start of every gesture in a 60 fps video, in seconds, one per line. The
# driver log times each synthesized touch exactly; the video fixes the offset
# between the two clocks -- the one that puts the most touches on the start of a
# burst of motion in the tab bar -- and then each gesture's own burst.
onsets() {
    "$PY" - "$1" "$2" "$3" "$4" <<'PY'
import re, sys, subprocess, time, datetime, numpy as np
video, xcb, count, startFile = sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4]
touches = [float(m.group(1)) for m in re.finditer(r"t =\s+([0-9.]+)s\s+Synthesize event", open(xcb).read())]
if len(touches) == count + 1:
    touches = touches[1:]    # the driver's warm-up tap before the schedule
if len(touches) != count:
    sys.exit("expected %d touches in %s, found %d" % (count, xcb, len(touches)))
size = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height",
                       "-of", "csv=p=0", video], capture_output=True, text=True, check=True).stdout.strip().split(",")
w = int(size[0]) // 6
h = int(int(size[1]) * 0.14) // 6
raw = subprocess.run(["ffmpeg", "-v", "error", "-i", video, "-vf",
                      "crop=iw:ih*0.14:0:ih*0.84,scale=%d:%d,format=gray" % (w, h), "-f", "rawvideo", "-"],
                     capture_output=True, check=True).stdout
n = len(raw) // (w * h)
fr = np.frombuffer(raw, np.uint8)[: n * w * h].reshape(n, h, w).astype(int)
d = np.abs(np.diff(fr, axis=0)).mean(axis=(1, 2))
# Gestures run on a fixed schedule, so matching bursts alone is ambiguous by whole
# periods. The clocks settle which period: the log's wall-clock test start plus a
# touch's offset, minus the recording's wall-clock start, is where that touch is in
# the video (the recorder starts a little late, so the video runs slightly behind).
m = re.search(r"Start Test at (\d{4}-\d\d-\d\d \d\d:\d\d:\d\d\.\d+)", open(xcb).read())
rec0 = float(open(startFile).read().strip())
test0 = time.mktime(datetime.datetime.strptime(m.group(1)[:23], "%Y-%m-%d %H:%M:%S.%f").timetuple()) \
    + float("0." + m.group(1).split(".")[1])
coarse = test0 - rec0
# The clocks are good to a few frames (measured: 0.07 s), and a burst they do not
# predict -- a stray repaint -- must not win. So every gesture is placed by the
# clocks on its own, independently of the others (one bad snap must not shift the
# rest), and only nudged to the first frame that moves within -0.1 / +0.25 s of that.
out = []
for tk in touches:
    pred = coarse + tk
    lo = max(0, int((pred - 0.1) * 60))
    hi = min(len(d), int((pred + 0.25) * 60) + 1)
    moving = [i for i in range(lo, hi) if d[i] > 0.5]
    out.append(moving[0] / 60.0 if moving else pred)
print("\n".join("%.4f" % t for t in out))
PY
}

label() {
    "$PY" - "$1" "$2" "$3" <<'PY'
import sys
from PIL import Image, ImageDraw, ImageFont
text, width, path = sys.argv[1], int(sys.argv[2]), sys.argv[3]
img = Image.new("RGBA", (width, 64), (0, 0, 0, 170))
d = ImageDraw.Draw(img)
try:
    font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 40)
except Exception:
    font = ImageFont.load_default()
tw = d.textlength(text, font=font)
d.text(((width - tw) / 2, 10), text, fill=(255, 255, 255, 255), font=font)
img.save(path)
PY
}

# Records one side (native or cn1) and keeps it only if every gesture's first 0.6 s
# is continuous motion. The simulator's recorder drops frames -- and the wall time
# they covered -- when the machine is loaded, which silently misplaces gestures;
# a dropped take is recorded again, up to three times.
record_side() {
    local side="$1" appearance="$2" count="$3" attempt
    for attempt in 1 2 3; do
        if [ "$side" = native ]; then
            xcrun simctl terminate "$UDID" "$CN1_ID" >/dev/null 2>&1 || true
            record "$WORK/native-$appearance.mov" "$appearance" ""
            xcrun simctl terminate "$UDID" "$PROBE_ID" >/dev/null 2>&1 || true
        else
            local home
            home="$(xcrun simctl get_app_container "$UDID" "$CN1_ID" data)/Documents/$CN1_ID"
            mkdir -p "$home"
            echo "$appearance" > "$home/tabs-showcase.txt"
            # The app's console goes beside the take, for diagnosing a bad one.
            xcrun simctl launch --console-pty --terminate-running-process "$UDID" "$CN1_ID" \
                > "$WORK/cn1-$appearance.console.txt" 2>&1 &
            sleep 6
            record "$WORK/cn1-$appearance.mov" "$appearance" "$CN1_ID"
            xcrun simctl terminate "$UDID" "$CN1_ID" >/dev/null 2>&1 || true
            rm -f "$home/tabs-showcase.txt"
        fi
        ffmpeg -v error -y -i "$WORK/$side-$appearance.mov" -vf fps=60 -c:v libx264 -crf 12 -pix_fmt yuv420p \
            "$WORK/$side-$appearance-60.mp4"
        local starts worst
        starts="$(onsets "$WORK/$side-$appearance-60.mp4" "$WORK/$side-$appearance.mov.xcb.txt" "$count" \
            "$WORK/$side-$appearance.mov.start")" || starts=""
        worst="$(continuity "$WORK/$side-$appearance-60.mp4" $starts)"
        if [ -n "$starts" ] && [ "$worst" -ge 18 ]; then
            return 0
        fi
        log "$side ($appearance) take $attempt dropped frames (fewest moving frames in a gesture: ${worst:-none}); again"
    done
    log "FATAL: $side ($appearance) could not be recorded without dropped frames (machine too loaded?)"
    exit 5
}

# The fewest changed frames in the first 0.6 s of any gesture (a clean take moves
# on nearly every frame there; a recorder drop shows as a jump).
continuity() {
    local video="$1"; shift
    [ $# -gt 0 ] || { echo 0; return; }
    "$PY" - "$video" "$@" <<'PY'
import sys, subprocess, numpy as np
video = sys.argv[1]
starts = [float(x) for x in sys.argv[2:]]
size = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height",
                       "-of", "csv=p=0", video], capture_output=True, text=True, check=True).stdout.strip().split(",")
w = int(size[0]) // 6
h = int(int(size[1]) * 0.14) // 6
raw = subprocess.run(["ffmpeg", "-v", "error", "-i", video, "-vf",
                      "crop=iw:ih*0.14:0:ih*0.84,scale=%d:%d,format=gray" % (w, h), "-f", "rawvideo", "-"],
                     capture_output=True, check=True).stdout
n = len(raw) // (w * h)
fr = np.frombuffer(raw, np.uint8)[: n * w * h].reshape(n, h, w).astype(int)
d = np.abs(np.diff(fr, axis=0)).mean(axis=(1, 2))
print(min(int((d[int(t * 60):int(t * 60) + 36] > 0.05).sum()) for t in starts))
PY
}

run_appearance() {
    local appearance="$1"
    local count
    count="$(echo "$SEQ" | tr ',' '\n' | grep -c .)"
    if [ -n "${TABS_REUSE:-}" ]; then
        # Recompose from an earlier run's raw recordings and driver logs.
        WORK="$TABS_REUSE"
    else
        log "Native ($appearance)"
        record_side native "$appearance" "$count"
        log "Codename One ($appearance)"
        record_side cn1 "$appearance" "$count"
    fi
    local on_native on_cn1
    on_native="$(onsets "$WORK/native-$appearance-60.mp4" "$WORK/native-$appearance.mov.xcb.txt" "$count" \
        "$WORK/native-$appearance.mov.start")"
    on_cn1="$(onsets "$WORK/cn1-$appearance-60.mp4" "$WORK/cn1-$appearance.mov.xcb.txt" "$count" \
        "$WORK/cn1-$appearance.mov.start")"
    log "gesture starts, native: $(echo $on_native)"
    log "gesture starts, Codename One: $(echo $on_cn1)"
    # Every gesture gets the same slot on both sides, starting a quarter second
    # before its first moving frame.
    local seg lead=0.25
    seg="$(awk -v p="$PERIOD_MS" 'BEGIN { printf "%.3f", p / 1000 - 0.3 }')"
    trims() {
        local input="$1" tag="$2" starts="$3" chain="" i=0 t
        for t in $starts; do
            chain="$chain[$input:v]trim=start=$(awk -v t="$t" -v l="$lead" 'BEGIN { s = t - l; if (s < 0) s = 0; printf "%.4f", s }'):duration=$seg,setpts=PTS-STARTPTS[$tag$i];"
            i=$((i + 1))
        done
        local joined="" j
        for ((j = 0; j < i; j++)); do joined="$joined[$tag$j]"; done
        echo "$chain${joined}concat=n=$i:v=1:a=0[$tag]"
    }
    local w=590
    label "iOS 27 native ($appearance)" "$w" "$WORK/l-native.png"
    label "Codename One ($appearance)" "$w" "$WORK/l-cn1.png"
    label "iOS 27 native" 1178 "$WORK/L-native.png"
    label "Codename One" 1178 "$WORK/L-cn1.png"
    local cut
    cut="$(trims 0 n "$on_native");$(trims 1 c "$on_cn1")"
    ffmpeg -v error -y -i "$WORK/native-$appearance-60.mp4" -i "$WORK/cn1-$appearance-60.mp4" \
        -i "$WORK/l-native.png" -i "$WORK/l-cn1.png" -filter_complex \
        "$cut;[n]scale=$w:-2[a0];[c]scale=$w:-2[b0];[a0][2:v]overlay=0:0[a];[b0][3:v]overlay=0:0[b];[a][b]hstack=inputs=2[v]" \
        -map "[v]" -r 60 -c:v libx264 -crf 16 -pix_fmt yuv420p "$OUT/tabs-side-by-side-$appearance.mp4"
    ffmpeg -v error -y -i "$WORK/native-$appearance-60.mp4" -i "$WORK/cn1-$appearance-60.mp4" \
        -i "$WORK/L-native.png" -i "$WORK/L-cn1.png" -filter_complex \
        "$cut;[n]crop=iw:512:0:ih-512[a0];[c]crop=iw:512:0:ih-512[b0];[a0][2:v]overlay=0:0[a];[b0][3:v]overlay=0:0[b];[a][b]vstack=inputs=2[v]" \
        -map "[v]" -r 60 -c:v libx264 -crf 14 -pix_fmt yuv420p "$OUT/tabs-side-by-side-$appearance-bar.mp4"
    log "Wrote $OUT/tabs-side-by-side-$appearance.mp4 and ...-bar.mp4"
}

case "$WHICH" in
    both) run_appearance light; run_appearance dark ;;
    light|dark) run_appearance "$WHICH" ;;
    *) log "appearance must be light, dark or both" >&2; exit 2 ;;
esac
