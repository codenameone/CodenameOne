#!/usr/bin/env bash
#
# Phase 0 spike for the CN1 device runtime: proves that a class which exists
# only at runtime can be dispatched to, type-checked, and survive collection --
# on a real iOS simulator and a real Android emulator.
#
# The two platforms need different mechanisms and this runs both:
#
#   iOS / ParparVM  runtime clazz synthesis. There is no defineClass and iOS
#                   forbids writing executable memory, but each class's vtable
#                   is heap-allocated and slot-indexed, so a subclass is built
#                   by copying the parent's clazz and repointing the overridden
#                   slots at an interpreter trampoline.
#                   Driver: vm/tests/src/test/resources/interp/cn1_interp_spike.c
#
#   Android / ART   build-time generated subclass plus real reflection. Dalvik
#                   has no patchable vtable and Play forbids loading dex at
#                   runtime, so the override guard is compiled in ahead of time.
#                   Driver: vm/tests/src/test/resources/interp/AndroidInterpSpike.java
#
# Both must print the same verdicts. Usage:
#   scripts/run-interp-spike.sh [ios|android|all]
#
# The iOS leg builds through the JUnit integration test, which translates with
# cn1.interpHost=true and links the spike into the generated sources; the
# Android leg compiles a dex and runs it with app_process.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-all}"

# Both `simctl ... booted` and a bare `adb shell` pick a device for you when
# more than one is running, and will happily pick the wrong one -- an iOS
# binary spawned into a booted watchOS simulator fails with a dyld_sim platform
# error that reads like a version mismatch. Always resolve a device explicitly.
pick_iphone() {
    xcrun simctl list devices booted \
        | grep -E "iPhone|iPad" \
        | head -1 \
        | sed -E 's/.*\(([0-9A-F-]{36})\).*/\1/'
}

pick_android() {
    adb devices | awk '/\tdevice$/ {print $1; exit}'
}

run_ios() {
    echo "== iOS simulator =="
    local udid
    udid="$(pick_iphone)"
    if [ -z "$udid" ]; then
        echo "no booted iPhone/iPad simulator; boot one with:" >&2
        echo "  xcrun simctl boot 'iPhone 17 Pro'" >&2
        return 1
    fi
    echo "device: $udid"

    # Translate + build + run on the host, which is also where the assertions
    # live. This leaves the generated C in a temp dir we then rebuild for the
    # simulator, so the simulator run uses exactly the code the test verified.
    ( cd "$REPO_ROOT/vm" && mvn -q -B \
        -Dmaven.repo.local="$REPO_ROOT/.m2-local" \
        -pl tests -am \
        -Dtest=InterpHostVtableSynthesisIntegrationTest \
        -Dsurefire.failIfNoSpecifiedTests=false test )

    local src
    src="$(ls -td "${TMPDIR:-/tmp}"/interp-vt-run*/dist/*-src 2>/dev/null | head -1)"
    if [ -z "$src" ]; then
        echo "no generated sources found; did the integration test run?" >&2
        return 1
    fi

    local out="${TMPDIR:-/tmp}/interp-spike-ios"
    mkdir -p "$out"
    local sdk
    sdk="$(xcrun --sdk iphonesimulator --show-sdk-path)"
    xcrun --sdk iphonesimulator clang \
        -target arm64-apple-ios17.0-simulator -isysroot "$sdk" \
        -I"$src" -o "$out/InterpVtApp" "$src"/*.c
    xcrun simctl spawn "$udid" "$out/InterpVtApp"
}

run_android() {
    echo "== Android emulator =="
    local serial
    serial="$(pick_android)"
    if [ -z "$serial" ]; then
        echo "no attached device/emulator; boot one with:" >&2
        echo "  \$ANDROID_HOME/emulator/emulator -avd <name> -no-window" >&2
        return 1
    fi
    echo "device: $serial"

    local d8
    d8="$(ls -d "$HOME"/Library/Android/sdk/build-tools/*/d8 2>/dev/null | tail -1)"
    if [ -z "$d8" ]; then
        echo "d8 not found under \$HOME/Library/Android/sdk/build-tools" >&2
        return 1
    fi

    # javac and d8 need different JDKs: the spike targets bytecode 8 (what the
    # CN1 Android port is built against), while d8 itself is compiled for 11+.
    # A single JAVA_HOME cannot satisfy both, and the failure mode is an
    # UnsupportedClassVersionError from inside d8 rather than anything about
    # the code being built.
    local d8_java=""
    for candidate in "${JAVA17_HOME:-}" \
                     "$(/usr/libexec/java_home -v 17 2>/dev/null || true)" \
                     "$(/usr/libexec/java_home -v 21 2>/dev/null || true)" \
                     "$(/usr/libexec/java_home 2>/dev/null || true)"; do
        if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ]; then
            d8_java="$candidate"
            break
        fi
    done
    if [ -z "$d8_java" ]; then
        echo "no JDK 11+ found for d8; set JAVA17_HOME" >&2
        return 1
    fi

    local work="${TMPDIR:-/tmp}/interp-spike-android"
    rm -rf "$work" && mkdir -p "$work/classes"
    javac -source 8 -target 8 -nowarn -d "$work/classes" \
        "$REPO_ROOT/vm/tests/src/test/resources/interp/AndroidInterpSpike.java" 2>/dev/null
    JAVA_HOME="$d8_java" "$d8" --output "$work" "$work"/classes/*.class

    adb -s "$serial" push "$work/classes.dex" /data/local/tmp/interpspike.dex >/dev/null
    adb -s "$serial" shell \
        "CLASSPATH=/data/local/tmp/interpspike.dex app_process / AndroidInterpSpike" \
        | tr -d '\r'
}

case "$TARGET" in
    ios)     run_ios ;;
    android) run_android ;;
    all)     run_ios; echo; run_android ;;
    *)       echo "usage: $0 [ios|android|all]" >&2; exit 2 ;;
esac
