#!/usr/bin/env bash
# Builds both sides of the benchmark for one platform, from a tree that
# prepare.sh has already materialised.
#
#   build_apps.sh --work /tmp/fbench --platform macos
#
# Prints the two artifact paths as `cn1=<path>` and `flutter=<path>` so the
# caller does not have to know each platform's output layout.
#
# RELEASE ON BOTH SIDES, always. A debug build of either is not a measurement:
# Flutter's debug engine carries the whole JIT, and a Codename One debug build
# keeps its assertions. Where a platform cannot produce a release build for
# both, this script fails rather than quietly measuring mismatched ones -- see
# the ios case, where Dart cannot compile ahead of time for the simulator at
# all.
#
# EVERY recipe here builds LOCALLY. The generated build.sh offers targets that
# send the build to the cloud builder -- `ios-device`, `android-device`,
# `mac-os-x-desktop` and friends -- which is credentialed, billed, and applies
# a 100MB artifact limit this application exceeds. The *-source targets
# generate an Xcode or Gradle project on the runner instead, and this script
# compiles those. Nothing here contacts the build server.
#
# Desktop is measured against the NATIVE ports, never against JavaSE. Codename
# One has two different desktop stories and only one of them is a fair
# comparison here: the javase targets bundle a JVM and ship an executable jar
# beside its dependencies, while the native targets compile through ParparVM to
# a real binary -- AppKit on macOS, clang-cl on Windows, CMake/Ninja with
# GTK3/Cairo on Linux. Flutter's desktop builds are native binaries, so the
# native ports are the like-for-like artifact and the javase ones are not.
#
# EXERCISED: none end to end yet. The Codename One side of `macos` has been
# built from a prepared tree; the native compile steps and the other platforms
# have not. Treat their first CI run as the thing under review.
set -Eeuo pipefail
# Report WHERE a failure happened. This script drives Maven, Flutter, pub and
# python, several of which can fail with no output at all -- the Windows leg
# twice reported an exit code and nothing else, and the first guess about
# which command produced it was wrong. -E so the trap is inherited by the
# subshells the build steps run in.
trap 'rc=$?; echo "build_apps.sh: FAILED at line $LINENO (exit $rc)" >&2' ERR

WORK=""
PLATFORM=""
MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-}"

while [ $# -gt 0 ]; do
  case "$1" in
    --work) WORK="$2"; shift 2 ;;
    --platform) PLATFORM="$2"; shift 2 ;;
    --maven-repo) MAVEN_REPO_LOCAL="$2"; shift 2 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

[ -n "$WORK" ] && [ -n "$PLATFORM" ] || {
  echo "usage: build_apps.sh --work <dir> --platform <id>" >&2; exit 2; }

FL="$WORK/flutter"
CN1="$WORK/cn1"
MVN_REPO_ARG=""
[ -n "$MAVEN_REPO_LOCAL" ] && MVN_REPO_ARG="-Dmaven.repo.local=$MAVEN_REPO_LOCAL"

# The Codename One build needs a display. Its CSS compiler (CN1CSSCLI) opens a
# JFrame, so on a headless runner `cn1:css` dies with HeadlessException after
# the transpile has already succeeded -- which reads like a Flutter problem and
# is not one. -Djava.awt.headless=true is NOT the fix: the toolchain genuinely
# uses AWT windowing, and asking for headless turns the crash into a different
# crash. On Linux that means xvfb, the way every other Linux leg in this
# repository runs Maven; the macOS and Windows runners have a display already.
XVFB=""
if [ "$(uname -s)" = "Linux" ] && [ -z "${DISPLAY:-}" ]; then
  command -v xvfb-run >/dev/null 2>&1 || {
    echo "xvfb-run is needed to build on a headless Linux host, and is absent." >&2
    echo "Install it (apt-get install xvfb) or run with a DISPLAY set." >&2
    exit 2; }
  XVFB="xvfb-run -a"
fi

cn1_build() {   # cn1_build <platform> <buildTarget> [extra maven args...]
  local plat="$1" target="$2"; shift 2
  ( cd "$CN1" && $XVFB mvn -B $MVN_REPO_ARG package -DskipTests \
      -DskipComplianceCheck=true \
      -Dcodename1.platform="$plat" -Dcodename1.buildTarget="$target" "$@" )
}

first() {       # first existing match, or empty
  find "$@" 2>/dev/null | head -1
}

case "$PLATFORM" in
  macos)
    # local-mac-device is the NATIVE AppKit app built on this machine, as
    # opposed to mac-os-x-native (the same build, submitted to the cloud) and
    # mac-os-x-desktop (the javase build, which bundles a JVM).
    ( cd "$FL" && flutter build macos --release )
    # UNSIGNED, on purpose and explicitly. MacOSBuildHints defaults both signing
    # identities to a real certificate rather than leaving them null, so a build
    # that names none still tries to sign and stops at "Signing for Bench
    # requires selecting a development team" -- after translating and compiling
    # the whole application. `none` is the sentinel the builder documents for
    # this, and it makes it pass CODE_SIGNING_ALLOWED=NO to xcodebuild, which is
    # what the ios recipe below does by hand. Both channels, because the two
    # default independently.
    #
    # Nothing here is distributed, so there is nothing to sign FOR: the binary
    # is measured and thrown away. Flutter's side is unsigned too.
    cn1_build ios local-mac-device \
        -Dcodename1.arg.macos.signingIdentity.appStore=none \
        -Dcodename1.arg.macos.signingIdentity.developerID=none
    echo "flutter=$FL/build/macos/Build/Products/Release/gallery.app"
    echo "cn1=$(first "$CN1/ios/target" -maxdepth 4 -name '*.app' -type d)"
    ;;

  ios)
    # Device release on both sides, unsigned. NOT the simulator: `flutter build
    # ios --simulator --release` is refused outright by the Flutter tool,
    # because Dart cannot AOT-compile for the simulator, so a simulator run
    # would compare Flutter's JIT debug engine against a release build.
    ( cd "$FL" && flutter build ios --release --no-codesign )
    # ios-source generates the Xcode project locally rather than sending the
    # build to the cloud builder.
    cn1_build ios ios-source
    XCPROJ="$(first "$CN1/ios/target" -maxdepth 2 -type d -name '*-ios-source')"
    [ -n "$XCPROJ" ] || { echo "no generated Xcode project" >&2; exit 2; }
    ( cd "$XCPROJ" && xcodebuild -project *.xcodeproj -configuration Release \
        -sdk iphoneos CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO \
        -derivedDataPath build build )
    echo "flutter=$FL/build/ios/iphoneos/Runner.app"
    echo "cn1=$(first "$XCPROJ/build/Build/Products" -maxdepth 2 -name '*.app')"
    ;;

  android)
    ( cd "$FL" && flutter build apk --release )
    cn1_build android android-source
    GRADLE_PROJECT="$(first "$CN1/android/target" -maxdepth 2 -type d -name '*-android-source')"
    [ -n "$GRADLE_PROJECT" ] || { echo "no generated Gradle project" >&2; exit 2; }
    ( cd "$GRADLE_PROJECT" && ./gradlew assembleRelease )
    echo "flutter=$FL/build/app/outputs/flutter-apk/app-release.apk"
    echo "cn1=$(first "$GRADLE_PROJECT" -name '*-release*.apk')"
    ;;

  linux)
    # ParparVM to a native ELF against GTK3/Cairo, built here rather than
    # submitted. Needs the GTK3 development packages and a C toolchain on the
    # runner; without them the Codename One half fails loudly rather than
    # falling back to a JVM build that would not be comparable.
    ( cd "$FL" && flutter build linux --release )
    cn1_build linux local-linux-device
    echo "flutter=$FL/build/linux/x64/release/bundle"
    echo "cn1=$(first "$CN1/linux/target" -maxdepth 4 -type d -name '*-linux-device')"
    ;;

  windows)
    # ParparVM to a native binary through clang-cl, built here rather than
    # submitted.
    ( cd "$FL" && flutter build windows --release )
    cn1_build win local-windows-device
    echo "flutter=$FL/build/windows/x64/runner/Release"
    echo "cn1=$(first "$CN1/win/target" -maxdepth 4 -type d -name '*-windows-device')"
    ;;

  javascript)
    ( cd "$FL" && flutter build web --release )
    # local-javascript, not the `javascript` target, which is the cloud build.
    cn1_build javascript local-javascript
    echo "flutter=$FL/build/web"
    echo "cn1=$(first "$CN1/javascript/target" -maxdepth 3 -type d -name 'javascript')"
    ;;

  *)
    echo "no build recipe for platform: $PLATFORM" >&2
    exit 2
    ;;
esac
