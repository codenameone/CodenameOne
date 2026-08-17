#!/usr/bin/env bash
# Builds the device runtime app for the iOS simulator, installs it, and pushes a
# program to it.
#
# Everything here is local: an interp-host build is an ordinary ios-source build
# with cn1.interpHost=true, compiled by the local Xcode for iphonesimulator with
# signing off. No build server and no certificate is involved, which is the
# point -- this is the loop a framework developer iterates in.
#
# Usage: run-device-runtime-ios.sh [--skip-build] [program.java]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP="$ROOT/scripts/cn1-device-runtime"
WORK="${CN1_DEVRUNTIME_WORK:-${TMPDIR:-/tmp}/cn1-devruntime-ios}"
mkdir -p "$WORK"

SKIP_BUILD=0
PROGRAM=""
while [ $# -gt 0 ]; do
    case "$1" in
        --skip-build) SKIP_BUILD=1 ;;
        *) PROGRAM="$1" ;;
    esac
    shift
done

# A booted simulator is not enough to identify one: `booted` resolves to
# whichever device simctl lists first, and a booted Apple Watch will happily
# take an install that then never appears on the phone. Pin the UDID.
SIM_UDID="${CN1_SIM_UDID:-}"
if [ -z "$SIM_UDID" ]; then
    SIM_UDID="$(xcrun simctl list devices available -j \
        | python3 -c '
import json,sys
d=json.load(sys.stdin)["devices"]
for runtime, devices in d.items():
    if "iOS" not in runtime:
        continue
    for dev in devices:
        if dev.get("state") == "Booted" and "iPhone" in dev["name"]:
            print(dev["udid"]); raise SystemExit
for runtime, devices in d.items():
    if "iOS" not in runtime:
        continue
    for dev in devices:
        if "iPhone" in dev["name"]:
            print(dev["udid"]); raise SystemExit
')"
fi
if [ -z "$SIM_UDID" ]; then
    echo "no iPhone simulator available; install one in Xcode > Settings > Components" >&2
    exit 1
fi
echo "simulator: $SIM_UDID"

# The simulator shares the host's loopback, so an `adb forward tcp:18234` left
# over from an Android run owns the port the iOS app wants and quietly wins the
# race. The push then reaches the Android emulator and fails with whatever that
# app makes of the bundle -- an error that says nothing about the real problem.
if command -v adb >/dev/null 2>&1 && adb forward --list 2>/dev/null | grep -q "tcp:18234"; then
    echo "clearing an adb forward that holds tcp:18234"
    adb forward --remove tcp:18234 >/dev/null 2>&1 || true
fi
# Both runtimes dial out to the same host port, so a device runtime still
# running on the emulator answers the push meant for the simulator and reports
# a perfectly good result for the wrong device. That mistake has been made here
# twice; stopping the Android app costs nothing and makes it impossible.
if command -v adb >/dev/null 2>&1 && [ -n "$(adb devices | sed -n '2p')" ]; then
    adb shell am force-stop com.codenameone.devruntime >/dev/null 2>&1 || true
fi
xcrun simctl bootstatus "$SIM_UDID" -b >/dev/null 2>&1 || xcrun simctl boot "$SIM_UDID" || true

MVN_ARGS=()
[ -n "${SETTINGS_LOCAL:-}" ] && MVN_ARGS+=(-s "$SETTINGS_LOCAL")
[ -n "${M2_LOCAL:-}" ] && MVN_ARGS+=(-Dmaven.repo.local="$M2_LOCAL")

SRC_DIR="$APP/ios/target/cn1-device-runtime-ios-1.0-SNAPSHOT-ios-source"

if [ "$SKIP_BUILD" = 0 ]; then
    # Two artifacts repackage things you might reasonably think live elsewhere,
    # and both have cost a full hour-long cycle to a change that "did nothing":
    #
    #   parparvm  carries the translator the build actually runs, so installing
    #             vm/ByteCodeTranslator alone leaves the old one in place.
    #   ios       bundles iOSPort.jar, which embeds a copy of the core classes,
    #             so a change to com.codename1.interp is invisible until this is
    #             rebuilt -- core alone is not enough.
    #
    # Rebuilding all three here costs a minute and removes the whole category.
    echo "=== refreshing core, translator and iOS port ==="
    # Built one POM at a time rather than as `-pl core,parparvm,ios` from the
    # aggregator: the aggregator reactor reads every module, and the archetype
    # modules need archetype-packaging, which an offline build against a local
    # repository does not have. Pointing at a module reads only its parent chain.
    for module in core parparvm ios; do
        (cd "$ROOT/maven" && mvn -q ${MVN_ARGS[@]+"${MVN_ARGS[@]}"} \
            -f "$module/pom.xml" install -DskipTests) || {
            echo "could not rebuild $module" >&2
            exit 1
        }
    done

    echo "=== translating (interp host) ==="
    # The generated Xcode project is not regenerated in place: the build copies
    # into it and leaves whatever is already there. A native source or a
    # translator header that changed since the last run would silently keep its
    # old contents and fail to compile against the new Java side, which reads as
    # "my edit had no effect" rather than as a stale copy.
    rm -rf "$SRC_DIR" "$APP/ios/target/codenameone"
    # The ios module lives behind a profile keyed on codename1.platform, and the
    # translator needs JDK 17 for javac while the framework itself was built
    # with 8; both are the project's normal arrangement, not something this
    # script invents.
    (cd "$APP" && JAVA_HOME="${JAVA17_HOME:-$JAVA_HOME}" \
        PATH="${JAVA17_HOME:-$JAVA_HOME}/bin:$PATH" \
        mvn -o ${MVN_ARGS[@]+"${MVN_ARGS[@]}"} package -DskipTests \
        -Dcodename1.platform=ios \
        -Dcodename1.buildTarget=ios-source \
        -Dcodename1.arg.ios.interpHost=true \
        -Dmaven.compiler.fork=true \
        -Dmaven.compiler.executable="${JAVA17_HOME:-$JAVA_HOME}/bin/javac" \
        -Dopen=false 2>&1 | tee "$WORK/translate.log" | grep -E "BUILD|ERROR" | head -20)

    echo "=== xcodebuild ==="
    (cd "$SRC_DIR" && xcodebuild -workspace CN1DeviceRuntime.xcworkspace \
        -scheme CN1DeviceRuntime -sdk iphonesimulator -configuration Debug \
        -destination "platform=iOS Simulator,id=$SIM_UDID" \
        -derivedDataPath "$WORK/dd" \
        CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build \
        > "$WORK/xcodebuild.log" 2>&1) || {
        echo "xcodebuild failed; last errors:" >&2
        grep -E "error:" "$WORK/xcodebuild.log" | head -20 >&2
        exit 1
    }
    echo "BUILD SUCCEEDED"
fi

APP_BUNDLE="$WORK/dd/Build/Products/Debug-iphonesimulator/CN1DeviceRuntime.app"
[ -d "$APP_BUNDLE" ] || { echo "no app bundle at $APP_BUNDLE" >&2; exit 1; }

echo "=== installing ==="
xcrun simctl uninstall "$SIM_UDID" com.codenameone.devruntime >/dev/null 2>&1 || true
xcrun simctl install "$SIM_UDID" "$APP_BUNDLE"

echo "=== launching ==="
xcrun simctl launch --console-pty "$SIM_UDID" com.codenameone.devruntime \
    > "$WORK/console.log" 2>&1 &
CONSOLE_PID=$!
trap 'kill $CONSOLE_PID 2>/dev/null || true' EXIT

# The listener binds during Lifecycle.init, after the framework has started; ten
# seconds is generous on a warm simulator and still fails fast on a cold one.
for _ in $(seq 1 20); do
    if grep -q "CN1SS:DEVRUNTIME" "$WORK/console.log" 2>/dev/null; then
        break
    fi
    sleep 1
done
if ! grep "CN1SS:DEVRUNTIME" "$WORK/console.log"; then
    # --console-pty wants a terminal and gives nothing without one, so fall back
    # to the unified log, where NSLog output lands either way.
    xcrun simctl spawn "$SIM_UDID" log show --last 3m --style compact \
        --predicate 'eventMessage CONTAINS "CN1SS:"' > "$WORK/unified.log" 2>/dev/null || true
    grep "CN1SS:DEVRUNTIME" "$WORK/unified.log" || {
        echo "the app never reported its device runtime status:" >&2
        tail -30 "$WORK/console.log" >&2
        exit 1
    }
fi

if [ -n "$PROGRAM" ]; then
    echo "=== pushing $PROGRAM ==="
    # The simulator shares the host's loopback, so the app's outbound dial
    # reaches this listener directly -- no port forwarding, unlike Android,
    # which needs `adb reverse` to see the host at 127.0.0.1.
    "$ROOT/scripts/cn1-push.sh" "$PROGRAM" 18234
    sleep 2
    tail -20 "$WORK/console.log"
fi
