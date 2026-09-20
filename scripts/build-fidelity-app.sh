#!/usr/bin/env bash
# Convenience wrapper to build the native-fidelity app for a platform by
# delegating to the existing per-platform CN1 build scripts with
# CN1_APP_DIR=scripts/fidelity-app. The fidelity app forces the iOS Metal
# pipeline and bundles the Material gradle dependency via its build hints.
#
# Usage: build-fidelity-app.sh <android|ios>
set -euo pipefail

PLATFORM="${1:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export CN1_APP_DIR="scripts/fidelity-app"

case "$PLATFORM" in
  android)
    exec "$SCRIPT_DIR/build-android-app.sh" "${@:2}"
    ;;
  ios)
    # The golden set names the OS design generation this build will be SCORED
    # against, so it also has to name the theme generation the app ships: the
    # iOS 26 and iOS 27 themes differ in 56 of the 68 reference tiles, and
    # scoring one against the other's goldens reports a regression in every one
    # of them. One variable drives the capture (build-ios-native-ref.sh), the
    # app build (here) and the comparison (run-ios-fidelity-tests.sh).
    #
    # It rides IOS_DEPENDENCY_ARGS because that is the only channel
    # build-ios-app.sh forwards to the inner mvnw -- its positional arguments go
    # nowhere.
    #
    # SETTING THE HINT IS NOT ENOUGH, and the ways it silently does nothing all
    # look identical from here: two runs whose scores match to the last decimal,
    # which reads as "the theme change had no effect" rather than "the theme was
    # never loaded". Every one of these was hit in practice:
    #
    #   - The suite installs its own theme (FidelityDeviceRunner.resolveThemeResource)
    #     instead of going through IOSImplementation.installNativeTheme, and used
    #     to hardcode generation 26. It now asks the port.
    #   - iOS has no generic build-hint bridge, so the hint only reaches the device
    #     as a static setter IPhoneBuilder writes into the generated stub. A stale
    #     codenameone-maven-plugin simply omits it.
    #   - The inner mvnw resolves that plugin from whatever localRepository is
    #     configured -- a machine-wide /tmp/cn1-local-repo shared between checkouts
    #     will hand it another checkout's jar. Pass -Dmaven.repo.local here too.
    #   - The generated stub is a plugin OUTPUT, and Maven does not know a plugin
    #     change invalidates it; an incremental build happily reuses the old stub.
    #
    # Checking that the hint's VALUE appears in the generated constant pool proves
    # none of this -- it only proves the string was interned. The two checks that
    # actually settle it:
    #
    #   grep setIosThemeGeneration <ios-source>/*-src/*Stub.m
    #   grep 'installed theme' artifacts/ios-fidelity/simctl-log.txt
    case "${CN1SS_FIDELITY_GOLDEN_SET:-}" in
      ios-27-*)
        export IOS_DEPENDENCY_ARGS="${IOS_DEPENDENCY_ARGS:-} -Dcodename1.arg.ios.themeMode=modern -Dcodename1.arg.ios.themeGeneration=27"
        echo "[build-fidelity-app] golden set ${CN1SS_FIDELITY_GOLDEN_SET}: building with ios.themeGeneration=27" >&2
        ;;
    esac
    exec "$SCRIPT_DIR/build-ios-app.sh" "${@:2}"
    ;;
  *)
    echo "Usage: $0 <android|ios>" >&2
    exit 2
    ;;
esac
