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
    ios_generation=26
    case "${CN1SS_FIDELITY_GOLDEN_SET:-}" in
      ios-27-*)
        ios_generation=27
        export IOS_DEPENDENCY_ARGS="${IOS_DEPENDENCY_ARGS:-} -Dcodename1.arg.ios.themeMode=modern -Dcodename1.arg.ios.themeGeneration=27"
        echo "[build-fidelity-app] golden set ${CN1SS_FIDELITY_GOLDEN_SET}: building with ios.themeGeneration=27" >&2
        ;;
    esac

    # Forward the local repository the OUTER build is using. The inner mvnw
    # otherwise resolves 8.0-SNAPSHOT from whatever localRepository is
    # configured for the machine, which on a box with several checkouts is a
    # shared directory another checkout last wrote -- and a plugin from there
    # simply omits setIosThemeGeneration, leaving the port on generation 26
    # while this script announces an iOS 27 build.
    #
    # Propagated, never invented: CI installs into the repository its own cache
    # restores and sets nothing here, so imposing a per-checkout default would
    # point the inner build at an empty directory and break it. Set
    # CN1_LOCAL_REPO to the repository that holds the artifacts you just built.
    if [ -n "${CN1_LOCAL_REPO:-}" ]; then
      export IOS_DEPENDENCY_ARGS="${IOS_DEPENDENCY_ARGS:-} -Dmaven.repo.local=${CN1_LOCAL_REPO}"
      echo "[build-fidelity-app] inner build uses maven.repo.local=${CN1_LOCAL_REPO}" >&2
    fi

    # Drop a generated Xcode project that was produced for a DIFFERENT theme
    # generation. CN1BuildMojo.doIOSLocalBuild() decides whether to regenerate
    # from source timestamps alone, so a build-hint change on the command line
    # does not reach it: the stub keeps its previous setIosThemeGeneration call,
    # the runner trusts that stale value and installs the other generation's
    # .res, and the run scores one generation against the other's goldens with
    # nothing in the output saying so. Measured while tuning gen27.css -- three
    # consecutive runs produced byte-identical scores for exactly this reason.
    #
    # An existing project with NO stamp is also dropped: its generation is
    # unknown, and one extra regeneration is cheaper than a silently mismatched
    # run.
    #
    # Removing the generated *-ios-source directory is NOT enough, measured: after
    # a change to a CORE class the rebuild returned in 36 seconds and the emitted
    # C still carried the previous constant, so something upstream of that
    # directory is reused even when it is gone. The whole module target goes,
    # along with the sources archive build-ios-app.sh writes beside it.
    #
    # Note the limit of this stamp: it keys on the GENERATION, so it does not fire
    # for a core edit at the same generation. Iterating on core and the fidelity
    # app together still needs a manual clean of these paths -- Maven does not
    # treat a changed dependency as invalidating generated sources.
    ios_target="$SCRIPT_DIR/fidelity-app/ios/target"
    ios_stamp="$ios_target/.cn1-theme-generation"
    if [ -d "$ios_target" ]; then
      if [ ! -f "$ios_stamp" ] || [ "$(cat "$ios_stamp" 2>/dev/null)" != "$ios_generation" ]; then
        echo "[build-fidelity-app] generation changed -> clearing $ios_target" >&2
        rm -rf "$ios_target"
        rm -f "$SCRIPT_DIR/../artifacts/bytecode-translator-sources.zip" 2>/dev/null || true
        mkdir -p "$ios_target"
      fi
      # Written before the build rather than after, so the stamp always
      # describes the project on disk: the project is absent at this point, so a
      # build that fails part way leaves "absent or generation N", never a
      # generation-N stamp over a generation-M project.
      printf '%s\n' "$ios_generation" > "$ios_stamp"
    fi
    exec "$SCRIPT_DIR/build-ios-app.sh" "${@:2}"
    ;;
  *)
    echo "Usage: $0 <android|ios>" >&2
    exit 2
    ;;
esac
