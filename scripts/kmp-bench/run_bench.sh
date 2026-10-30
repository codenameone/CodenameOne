#!/usr/bin/env bash
#
# Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.
#
# Builds the Keynote app twice from the same Compose source -- once with
# Codename One (compose-compat, ParparVM) and once with Compose Multiplatform
# (Kotlin/Native + Skia) -- and a third time from app/java, the same screens
# written in Java over Codename One directly. Runs all three on one iOS
# simulator and writes results.json and results.md into the work directory.
#
#   run_bench.sh --work <dir> [--runs N] [--device <udid>] [--maven-repo <dir>]
#
# Start-up is process start to marker: each app prints "KMPBENCH first-frame
# <epoch ms>" from the first main-thread turn after its UI is installed, and the
# start is the kernel's start time for the process, so simctl's own latency is
# in neither. Compose Multiplatform's marker runs before Skia has drawn, which
# favours it. Memory is physical footprint (footprint(1)) a few seconds after
# launch; resident size counts the shared dyld cache on the simulator. Run 1 of
# each is cold (first launch after install) and reported separately.
set -Eeuo pipefail
trap 'rc=$?; echo "run_bench.sh: FAILED at line $LINENO (exit $rc)" >&2' ERR

WORK=""; RUNS=5; DEVICE=""; MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-}"
while [ $# -gt 0 ]; do
  case "$1" in
    --work) WORK="$2"; shift 2 ;;
    --runs) RUNS="$2"; shift 2 ;;
    --device) DEVICE="$2"; shift 2 ;;
    --maven-repo) MAVEN_REPO_LOCAL="$2"; shift 2 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
[ -n "$WORK" ] || { echo "usage: run_bench.sh --work <dir> [--runs N]" >&2; exit 2; }
HERE="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$WORK"; WORK="$(cd "$WORK" && pwd)"
MVN_REPO_ARG=""
[ -n "$MAVEN_REPO_LOCAL" ] && MVN_REPO_ARG="-Dmaven.repo.local=$MAVEN_REPO_LOCAL"
: "${JAVA17_HOME:?JAVA17_HOME must name a JDK 17}"

"$HERE/app/prepare.sh" --work "$WORK" ${MAVEN_REPO_LOCAL:+--maven-repo "$MAVEN_REPO_LOCAL"}

echo "== Codename One: translate and build"
( cd "$WORK/cn1" && JAVA_HOME="$JAVA17_HOME" mvn -B -q $MVN_REPO_ARG package -DskipTests \
    -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source -Dopen=false ) > "$WORK/cn1-build.log" 2>&1
SRC="$(dirname "$(find "$WORK/cn1/common/target" -maxdepth 2 -name Bench.xcodeproj | head -1)")"
( cd "$SRC" && xcodebuild -project Bench.xcodeproj -scheme Bench -configuration Release \
    -sdk iphonesimulator -arch arm64 -derivedDataPath "$WORK/dd" CODE_SIGNING_ALLOWED=NO build ) \
    > "$WORK/cn1-xcode.log" 2>&1
CN1_APP="$WORK/dd/Build/Products/Release-iphonesimulator/Bench.app"
CN1_EXE="$CN1_APP/Bench"
CN1_ID=com.example.kmpbench

echo "== Codename One, Java: translate and build"
( cd "$WORK/java" && JAVA_HOME="$JAVA17_HOME" mvn -B -q $MVN_REPO_ARG package -DskipTests \
    -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source -Dopen=false ) > "$WORK/java-build.log" 2>&1
JSRC="$(dirname "$(find "$WORK/java/common/target" -maxdepth 2 -name JavaBench.xcodeproj | head -1)")"
( cd "$JSRC" && xcodebuild -project JavaBench.xcodeproj -scheme JavaBench -configuration Release \
    -sdk iphonesimulator -arch arm64 -derivedDataPath "$WORK/dd-java" CODE_SIGNING_ALLOWED=NO build ) \
    > "$WORK/java-xcode.log" 2>&1
JAVA_APP="$WORK/dd-java/Build/Products/Release-iphonesimulator/JavaBench.app"
JAVA_EXE="$JAVA_APP/JavaBench"
JAVA_ID=com.example.javabench

echo "== Compose Multiplatform: build"
( cd "$WORK/cmp" && JAVA_HOME="$JAVA17_HOME" gradle --no-daemon -q \
    :composeApp:linkReleaseExecutableIosSimulatorArm64 ) > "$WORK/cmp-build.log" 2>&1
# An executable, not a framework in an Xcode project: the bundle is the
# executable plus an Info.plist, which is all a simulator app needs.
CMP_APP="$WORK/Keynote.app"; CMP_EXE="$CMP_APP/Keynote"; CMP_ID=com.example.kmpbench.cmp
rm -rf "$CMP_APP"; mkdir -p "$CMP_APP"
cp "$WORK/cmp/composeApp/build/bin/iosSimulatorArm64/releaseExecutable/composeApp.kexe" "$CMP_EXE"
cp "$HERE/app/cmp/Info.plist" "$CMP_APP/Info.plist"
codesign -s - --force "$CMP_APP" > /dev/null 2>&1

stripped() { cp "$1" "$WORK/strip.tmp"; strip "$WORK/strip.tmp" 2>/dev/null || true; stat -f %z "$WORK/strip.tmp"; }
# The bundle as shipped: a copy of the .app with its executable stripped.
bundle() {
  rm -rf "$WORK/bundle.tmp"; cp -R "$1" "$WORK/bundle.tmp"
  strip "$WORK/bundle.tmp/$2" 2>/dev/null || true
  du -sk "$WORK/bundle.tmp" | awk '{print $1 * 1024}'
}
# Bytes of everything in the bundle but the executable (fonts, theme, plists).
resources() { find "$1" -type f ! -path "$1/$2" -exec stat -f %z {} + | awk '{s += $1} END {print s + 0}'; }
largest() { find "$1" -type f ! -path "$1/$2" -exec stat -f '%z %N' {} + | sort -rn | head -5 | sed "s|$1/||"; }

if [ -z "$DEVICE" ]; then
  DEVICE="$(xcrun simctl list devices available | sed -n 's/.*iPhone 17 Pro (\([0-9A-F-]*\)).*/\1/p' | head -1)"
fi
[ -n "$DEVICE" ] || { echo "no iPhone simulator found; pass --device" >&2; exit 1; }
xcrun simctl boot "$DEVICE" 2>/dev/null || true
trap 'xcrun simctl shutdown "$DEVICE" 2>/dev/null || true' EXIT
xcrun simctl bootstatus "$DEVICE" > /dev/null

# The kernel's start time of a process, epoch milliseconds.
started() {
  python3 - "$1" <<'PY'
import ctypes, ctypes.util, struct, sys
libc = ctypes.CDLL(ctypes.util.find_library("c"))
mib = (ctypes.c_int * 4)(1, 14, 1, int(sys.argv[1]))  # CTL_KERN, KERN_PROC, KERN_PROC_PID
buf = ctypes.create_string_buffer(1024); size = ctypes.c_size_t(1024)
libc.sysctl(mib, 4, buf, ctypes.byref(size), None, 0)
sec, usec = struct.unpack_from("qi", buf.raw, 0)  # kp_proc.p_starttime
print(sec * 1000 + usec // 1000)
PY
}
# Prints one start-up time per run, milliseconds, space separated.
measure() {
  local id="$1" name="$2" exe="$3" out=""
  for i in $(seq 1 "$RUNS"); do
    xcrun simctl terminate "$DEVICE" "$id" 2>/dev/null || true
    local log="$WORK/$name-run$i.log" t0 t
    ( timeout 20 xcrun simctl launch --console-pty "$DEVICE" "$id" > "$log" 2>&1 || true ) &
    for _ in $(seq 1 100); do grep -q "KMPBENCH first-frame" "$log" 2>/dev/null && break; sleep 0.1; done
    t="$(grep -o 'KMPBENCH first-frame [0-9]*' "$log" | awk '{print $3}' | head -1)"
    [ -n "$t" ] || { echo "$name run $i printed no marker; see $log" >&2; exit 1; }
    t0="$(started "$(pgrep -f "$exe" | head -1)")"
    out="$out $((t - t0))"
    sleep 3
    # While the app is still up: a launch is killed by its timeout after 20s.
    [ "$i" = "$RUNS" ] && xcrun simctl io "$DEVICE" screenshot "$WORK/$name.png" > /dev/null 2>&1 || true
  done
  echo "$out"
}
footprint_bytes() {
  xcrun simctl launch "$DEVICE" "$1" > /dev/null 2>&1; sleep 5
  footprint "$(pgrep -f "$2" | head -1)" 2>&1 \
    | awk '/Footprint:/ { for (i = 1; i <= NF; i++) if ($i == "Footprint:") {
        v = $(i + 1); u = $(i + 2); print int(v * (u == "GB" ? 1073741824 : u == "MB" ? 1048576 : 1024)); exit } }'
}

for side in cn1 java cmp; do
  if [ $side = cn1 ]; then app="$CN1_APP"; id=$CN1_ID; exe=/Bench.app/Bench
  elif [ $side = java ]; then app="$JAVA_APP"; id=$JAVA_ID; exe=JavaBench.app/JavaBench
  else app="$CMP_APP"; id=$CMP_ID; exe=Keynote.app/Keynote; fi
  xcrun simctl uninstall "$DEVICE" "$id" 2>/dev/null || true
  xcrun simctl install "$DEVICE" "$app"
  eval "STARTS_$side=\"\$(measure \"\$id\" \"\$side\" \"\$exe\")\""
done
# Runtime measurements (app/shared/commonMain/kotlin/keynote/Perf.kt): one
# launch per run with KMPBENCH_PERF=1, values in microseconds, collected as one
# "name=v1,v2,..." line per measure.
perf() {
  local id="$1" name="$2"
  for i in $(seq 1 "$RUNS"); do
    xcrun simctl terminate "$DEVICE" "$id" 2>/dev/null || true
    local log="$WORK/$name-perf$i.log"
    ( SIMCTL_CHILD_KMPBENCH_PERF=1 timeout 180 xcrun simctl launch --console-pty "$DEVICE" "$id" > "$log" 2>&1 || true ) &
    for _ in $(seq 1 1700); do grep -q "KMPBENCH perf-done" "$log" 2>/dev/null && break; sleep 0.1; done
    grep -q "KMPBENCH perf-done" "$log" || { echo "$name perf run $i did not finish; see $log" >&2; exit 1; }
    xcrun simctl terminate "$DEVICE" "$id" 2>/dev/null || true
  done
  cat "$WORK/$name"-perf[0-9]*.log | tr -d '\r' | awk '{ for (j = 1; j < NF; j++) if ($j == "KMPBENCH") break }
      j < NF && $(j + 1) !~ /^(perf-done|perf-start|first-frame)$/ {
      v[$(j + 1)] = v[$(j + 1)] (v[$(j + 1)] == "" ? "" : ",") $(j + 2) } END { for (k in v) print k "=" v[k] }' > "$WORK/$name-perf.txt"
}
perf $CN1_ID cn1; perf $JAVA_ID java; perf $CMP_ID cmp
CN1_RSS="$(footprint_bytes $CN1_ID /Bench.app/Bench)"; JAVA_RSS="$(footprint_bytes $JAVA_ID JavaBench.app/JavaBench)"; CMP_RSS="$(footprint_bytes $CMP_ID Keynote.app/Keynote)"

python3 - "$WORK" <<EOF
import json, statistics, sys
work = sys.argv[1]
def perfvals(name):
    out = {}
    for line in open(work + "/" + name + "-perf.txt"):
        k, v = line.strip().split("=")
        out[k] = statistics.median([int(x) for x in v.split(",")])
    return out
def side(starts, exe, bundle, rss, res, perf):
    s = [int(x) for x in starts.split()]
    warm = s[1:] or s
    return {"cold_start_ms": s[0], "warm_start_median_ms": statistics.median(warm), "starts_ms": s,
            "executable_stripped_bytes": exe, "bundle_bytes": bundle, "bundle_without_executable_bytes": res,
            "footprint_bytes": rss, "perf_median_of_runs_us": perfvals(perf)}
r = {"codename_one": side("$STARTS_cn1", $(stripped "$CN1_EXE"), $(bundle "$CN1_APP" Bench), ${CN1_RSS:-0},
                         $(resources "$CN1_APP" Bench), "cn1"),
     "codename_one_java": side("$STARTS_java", $(stripped "$JAVA_EXE"), $(bundle "$JAVA_APP" JavaBench), ${JAVA_RSS:-0},
                         $(resources "$JAVA_APP" JavaBench), "java"),
     "compose_multiplatform": side("$STARTS_cmp", $(stripped "$CMP_EXE"), $(bundle "$CMP_APP" Keynote), ${CMP_RSS:-0},
                         $(resources "$CMP_APP" Keynote), "cmp")}
json.dump(r, open(work + "/results.json", "w"), indent=2)
mb = lambda b: "%.1f MB" % (b / 1048576.0)
rows = [("Cold start", lambda x: "%d ms" % x["cold_start_ms"]),
        ("Warm start (median)", lambda x: "%d ms" % x["warm_start_median_ms"]),
        ("Executable, stripped", lambda x: mb(x["executable_stripped_bytes"])),
        ("App bundle, executable stripped", lambda x: mb(x["bundle_bytes"])),
        ("Bundle without executable", lambda x: mb(x["bundle_without_executable_bytes"])),
        ("Physical footprint", lambda x: mb(x["footprint_bytes"]))]
ms = lambda k: (lambda x: "%.2f ms" % (x["perf_median_of_runs_us"][k] / 1000.0))
rows += [("Recomposition, day change to drawn (median)", ms("recompose-median-us")),
         ("Recomposition, day change to drawn (p95)", ms("recompose-p95-us")),
         ("Scroll, work per frame (median)", ms("scroll-work-median-us")),
         ("Scroll, work per frame (p95)", ms("scroll-work-p95-us")),
         ("Scroll, frame interval (median)", ms("scroll-frame-median-us")),
         ("Scroll, frame interval (p95)", ms("scroll-frame-p95-us")),
         ("Scroll, frames over 2x median", lambda x: "%d of %d" % (x["perf_median_of_runs_us"]["scroll-jank-frames"],
                                                               x["perf_median_of_runs_us"]["scroll-frames"]))]
md = ["| | Codename One, Compose | Codename One, Java | Compose Multiplatform |", "|---|---|---|---|"]
md += ["| %s | %s | %s | %s |" % (n, f(r["codename_one"]), f(r["codename_one_java"]), f(r["compose_multiplatform"]))
       for n, f in rows]
open(work + "/results.md", "w").write("\n".join(md) + "\n")
print("\n".join(md))
EOF
echo "== Largest files besides the executable"
echo "-- Codename One"; largest "$CN1_APP" Bench
echo "-- Codename One, Java"; largest "$JAVA_APP" JavaBench
echo "-- Compose Multiplatform"; largest "$CMP_APP" Keynote
