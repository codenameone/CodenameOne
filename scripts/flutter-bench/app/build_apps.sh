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

HERE="$(cd "$(dirname "$0")" && pwd -P)"
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

# The Flutter entry point, on every platform. prepare.sh copies our
# main_bench.dart into the project beside the gallery's own main.dart, and it is
# the one that prints the start-up markers the harness times against -- but a
# bare `flutter build` builds lib/main.dart, so the Flutter side of every
# marker-timed platform printed nothing and was never measured. Android hid it:
# `am start -W` times the launch without markers.
FLUTTER_ENTRY="lib/main_bench.dart"

# Kept so a recipe can read what the builder REPORTS rather than guess at its
# layout; see cn1_reported.
CN1_LOG="$WORK/cn1-build.log"

cn1_build() {   # cn1_build <platform> <buildTarget> [extra maven args...]
  local plat="$1" target="$2"; shift 2
  # retry.sh for Central's transient 403/429 only; a build failure is not retried.
  ( cd "$CN1" && RETRY_ONLY_MATCHING=transient $XVFB bash "$HERE/../../ci/retry.sh" \
      mvn -B $MVN_REPO_ARG package -DskipTests \
      -DskipComplianceCheck=true \
      -Dcodename1.platform="$plat" -Dcodename1.buildTarget="$target" "$@" ) 2>&1 | tee "$CN1_LOG"
}


emit() {  # emit <side> <path>: hand an artifact path to the workflow, or fail
  # A missing artifact FAILS the build. An empty path used to reach the harness,
  # which reported the platform "not measured" and let the job pass -- macOS and
  # JavaScript were both green that way without measuring anything.
  [ -n "$2" ] && [ -e "$2" ] || {
    echo "no $1 artifact to measure (got '${2}')" >&2; exit 2; }
  printf '%s=%s\n' "$1" "$2"
}

cn1_reported() {  # cn1_reported <phrase>: the path a builder logged after <phrase>
  # The native Linux and Windows builders print where they put the binary
  # ("Built native Linux executable: <path>"). The recipes used to look for a
  # "*-<target>" directory instead, a layout those builders never produce --
  # they write to a "result" directory -- so the artifact path came back empty.
  local path
  path="$(sed -n "s/.*$1 //p" "$CN1_LOG" | tail -1 | tr -d '\r')"
  # On Windows the builder logs D:\a\...\Bench.exe. Git Bash's dirname does not
  # split on a backslash, so it is normalized to D:/a/.../Bench.exe first, a
  # form both this shell and the native Windows Python downstream accept.
  if [ -n "$path" ] && command -v cygpath >/dev/null 2>&1; then
    path="$(cygpath -m "$path")"
  fi
  [ -n "$path" ] || { echo "the Codename One build did not report: $1" >&2; exit 2; }
  [ -e "$path" ] || {
    echo "the Codename One build reported '$1 $path', which no longer exists" >&2; exit 2; }
  printf '%s\n' "$path"
}

sign_for_install() {  # sign_for_install <apk>: prints an installable apk path
  # assembleRelease with no signing configuration produces
  # app-release-unsigned.apk, which adb refuses to install at all. Flutter's
  # release template signs with the debug key, so signing ours with the same
  # key is the like-for-like choice, and it keeps the sizes comparable: both
  # sides then carry a signature block.
  local apk="$1" signer keystore out
  signer="$(ls -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)"
  [ -n "$signer" ] || { echo "apksigner not found under the Android SDK" >&2; exit 2; }
  if "$signer" verify "$apk" >/dev/null 2>&1; then
    printf '%s\n' "$apk"; return
  fi
  keystore="$HOME/.android/debug.keystore"
  if [ ! -f "$keystore" ]; then
    mkdir -p "$(dirname "$keystore")"
    keytool -genkeypair -keystore "$keystore" -storepass android -keypass android \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" >/dev/null
  fi
  out="${apk%.apk}-debugsigned.apk"
  "$signer" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android \
      --ks-key-alias androiddebugkey --out "$out" "$apk"
  printf '%s\n' "$out"
}
first() {       # first existing match, or empty
  find "$@" 2>/dev/null | head -1
}

case "$PLATFORM" in
  macos)
    # local-mac-device is the NATIVE AppKit app built on this machine, as
    # opposed to mac-os-x-native (the same build, submitted to the cloud) and
    # mac-os-x-desktop (the javase build, which bundles a JVM).
    ( cd "$FL" && flutter build macos --release -t "$FLUTTER_ENTRY" )
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
    emit flutter "$FL/build/macos/Build/Products/Release/gallery.app"
    # What the builder reports, not a search: the app is six levels down, and
    # a four-level search found nothing and left the platform "not measured".
    emit cn1 "$(cn1_reported 'Built native macOS application:')"
    ;;

  ios)
    # Device release on both sides, unsigned. NOT the simulator: `flutter build
    # ios --simulator --release` is refused outright by the Flutter tool,
    # because Dart cannot AOT-compile for the simulator, so a simulator run
    # would compare Flutter's JIT debug engine against a release build.
    ( cd "$FL" && flutter build ios --release -t "$FLUTTER_ENTRY" --no-codesign )
    # ios-source generates the Xcode project locally rather than sending the
    # build to the cloud builder.
    cn1_build ios ios-source
    XCPROJ="$(first "$CN1/ios/target" -maxdepth 2 -type d -name '*-ios-source')"
    [ -n "$XCPROJ" ] || { echo "no generated Xcode project" >&2; exit 2; }
    # BY TARGET, and with an explicit output directory.
    #
    # -derivedDataPath is refused without -scheme ("The flag -scheme,
    # -testProductsPath, or -xctestrun is required when specifying
    # -derivedDataPath"), and a Codename One project does not ship a shared
    # scheme -- scripts/ios/create-shared-scheme.py exists precisely because
    # one has to be created. That helper also wires a UI test bundle, which a
    # size measurement has no use for. Targets are always present, and
    # CONFIGURATION_BUILD_DIR puts the .app somewhere known instead of a hashed
    # DerivedData directory, which is all -derivedDataPath was for.
    TARGET="$( cd "$XCPROJ" && xcodebuild -list -project *.xcodeproj -json 2>/dev/null \
        | python3 -c 'import json,sys
try:
    project = json.load(sys.stdin).get("project", {})
except ValueError:
    project = {}
targets = project.get("targets") or []
name = project.get("name")
# The application target, which is the one named after the project; the first
# target otherwise. Picking a test or extension target would measure the wrong
# binary rather than fail, so the preference is explicit.
print(name if name in targets else (targets[0] if targets else ""))' )"
    [ -n "$TARGET" ] || {
      echo "the generated Xcode project declares no target to build" >&2; exit 2; }
    ( cd "$XCPROJ" && xcodebuild -project *.xcodeproj -target "$TARGET" \
        -configuration Release -sdk iphoneos \
        CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO \
        CONFIGURATION_BUILD_DIR="$XCPROJ/build" build )
    emit flutter "$FL/build/ios/iphoneos/Runner.app"
    # Directly under the build directory, because CONFIGURATION_BUILD_DIR put
    # it there; the Build/Products/... nesting above it is DerivedData's layout,
    # which this no longer uses.
    CN1_APP="$(first "$XCPROJ/build" -maxdepth 1 -name '*.app')"
    [ -n "$CN1_APP" ] || {
      echo "xcodebuild reported success but produced no .app under" >&2
      echo "$XCPROJ/build" >&2; exit 2; }
    emit cn1 "$CN1_APP"
    ;;

  android)
    # Retried ONLY for a failed SDK download. Flutter's Gradle build installs
    # the NDK it needs on first use, and one run got a corrupt archive from the
    # SDK server ("Archive is not a ZIP archive", "Failed to install the
    # following SDK components") after earlier runs had fetched the same NDK
    # cleanly. Any other failure is a real one and is not retried.
    ( cd "$FL" && RETRY_ATTEMPTS=3 \
        RETRY_ONLY_MATCHING='Failed to install the following SDK components|Archive is not a ZIP archive|Error on ZipFile unknown archive' \
        bash "$HERE/../../ci/retry.sh" flutter build apk --release -t "$FLUTTER_ENTRY" )
    cn1_build android android-source
    GRADLE_PROJECT="$(first "$CN1/android/target" -maxdepth 2 -type d -name '*-android-source')"
    [ -n "$GRADLE_PROJECT" ] || { echo "no generated Gradle project" >&2; exit 2; }
    ( cd "$GRADLE_PROJECT" && ./gradlew assembleRelease )
    emit flutter "$FL/build/app/outputs/flutter-apk/app-release.apk"
    CN1_APK="$(first "$GRADLE_PROJECT" -name '*-release*.apk' -not -name '*-debugsigned.apk')"
    [ -n "$CN1_APK" ] || { echo "assembleRelease produced no apk" >&2; exit 2; }
    emit cn1 "$(sign_for_install "$CN1_APK")"
    ;;

  linux)
    # ParparVM to a native ELF against GTK3/Cairo, built here rather than
    # submitted. Needs the GTK3 development packages and a C toolchain on the
    # runner; without them the Codename One half fails loudly rather than
    # falling back to a JVM build that would not be comparable.
    ( cd "$FL" && flutter build linux --release -t "$FLUTTER_ENTRY" )
    cn1_build linux local-linux-device
    emit flutter "$FL/build/linux/x64/release/bundle"
    # The result DIRECTORY: the executable plus the libraries it ships beside
    # itself, which count toward both installed and code size.
    emit cn1 "$(dirname "$(cn1_reported 'Built native Linux executable:')")"
    ;;

  windows)
    # ParparVM to a native binary through clang-cl, built here rather than
    # submitted.
    ( cd "$FL" && flutter build windows --release -t "$FLUTTER_ENTRY" )
    cn1_build win local-windows-device
    emit flutter "$FL/build/windows/x64/runner/Release"
    emit cn1 "$(dirname "$(cn1_reported 'Built native Windows executable:')")"
    ;;

  javascript)
    ( cd "$FL" && flutter build web --release -t "$FLUTTER_ENTRY" )
    # local-javascript, not the `javascript` target, which is the cloud build.
    cn1_build javascript local-javascript
    emit flutter "$FL/build/web"
    # The deployable war, because it is the one that SURVIVES the build: the
    # "browser bundle" zip is written inside target/codenameone/antProject,
    # a scratch project removed when the build ends, and only the war is
    # copied out to target/. A war's root is the static site a browser loads;
    # WEB-INF and META-INF are the server side, which no browser downloads, so
    # they are left out of what is sized.
    WEB_ZIP="$(cn1_reported 'JavaScript deployable bundle written to')"
    rm -rf "$WORK/cn1-web"; mkdir -p "$WORK/cn1-web"
    # Unpacked whole and pruned, rather than `unzip -x`: an exclusion pattern
    # that matches nothing is exit 11 for some unzip builds, which would end a
    # build over a folder a particular war happens not to have.
    ( cd "$WORK/cn1-web" && unzip -q "$WEB_ZIP" && rm -rf WEB-INF META-INF )
    WEB_INDEX="$(find "$WORK/cn1-web" -name index.html | awk '{ print length, $0 }' | sort -n | head -1 | cut -d' ' -f2-)"
    [ -n "$WEB_INDEX" ] || { echo "the browser bundle has no index.html" >&2; exit 2; }
    emit cn1 "$(dirname "$WEB_INDEX")"
    ;;

  *)
    echo "no build recipe for platform: $PLATFORM" >&2
    exit 2
    ;;
esac
