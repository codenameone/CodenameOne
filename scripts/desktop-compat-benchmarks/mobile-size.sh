#!/usr/bin/env bash
#
# Size of an imported desktop application as an iOS or an Android application.
#
#   mobile-size.sh --target ios|android --project <dir> --work <dir>
#
# --project is the Codename One project bench.sh generated and imported into
# (<work>/<name>/cn1/app); it is copied, never built in place. Nothing is
# installed or launched: iOS is an unsigned Release build for a generic device,
# Android the release APK (R8 on, signed with the throwaway key the generated
# project carries). Sizes only -- no simulator or emulator is involved.
#
# Environment: JAVA17_HOME (required), BENCH_M2 (Maven repository holding
# Codename One), BENCH_USER_HOME (keeps the plugin out of ~/.codenameone),
# ANDROID_HOME (Android only; the SDK is read, never added to).
#
# Writes <work>/<target>-size.json and keeps the build logs beside it.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET=""
PROJECT=""
WORK=""
while [ $# -gt 0 ]; do
  case "$1" in
    --target) TARGET="$2"; shift 2 ;;
    --project) PROJECT="$2"; shift 2 ;;
    --work) WORK="$2"; shift 2 ;;
    -h|--help) sed -n '2,18p' "$0"; exit 0 ;;
    *) echo "mobile-size: unknown argument $1" >&2; exit 2 ;;
  esac
done
if [ -z "$TARGET" ] || [ -z "$PROJECT" ] || [ -z "$WORK" ]; then
  echo "mobile-size: --target, --project and --work are required" >&2
  exit 2
fi
: "${JAVA17_HOME:?JAVA17_HOME must point at a JDK 17}"

say() { printf '[mobile-size %s] %s\n' "$TARGET" "$*"; }

mkdir -p "$WORK/tmp"
WORK="$(cd "$WORK" && pwd)"
TMP="$WORK/tmp"
rm -rf "$WORK/app"
cp -R "$PROJECT" "$WORK/app"
find "$WORK/app" -type d -name target -prune -exec rm -rf {} +

# -Dopen=false: the Codename One plugin otherwise opens a generated Xcode or
# Android Studio project in the IDE, and a benchmark must never open a window.
MVN_ARGS=(-B -ntp -Dopen=false)
if [ -n "${BENCH_M2:-}" ]; then
  MVN_ARGS+=("-Dmaven.repo.local=$BENCH_M2")
fi
MVN_JVM="-Djava.io.tmpdir=$TMP"
if [ -n "${BENCH_USER_HOME:-}" ]; then
  MVN_JVM="$MVN_JVM -Duser.home=$BENCH_USER_HOME"
fi
generate() { # <platform> <build target> <log>
  (cd "$WORK/app" && JAVA_HOME="$JAVA17_HOME" PATH="$JAVA17_HOME/bin:$PATH" MAVEN_OPTS="$MVN_JVM" \
    TMPDIR="$TMP" mvn "${MVN_ARGS[@]}" package -DskipTests \
    "-Dcodename1.platform=$1" "-Dcodename1.buildTarget=$2") > "$3" 2>&1
}

case "$TARGET" in
  ios)
    say "generating the Xcode project"
    generate ios ios-source "$WORK/ios-source.log"
    XCODEPROJ="$(find "$WORK/app" -type d -name '*.xcodeproj' -not -path '*/Pods/*' | head -1)"
    [ -n "$XCODEPROJ" ] || { echo "mobile-size: no Xcode project was generated" >&2; exit 1; }
    NAME="$(basename "$XCODEPROJ" .xcodeproj)"
    say "xcodebuild Release, generic device, unsigned"
    (cd "$(dirname "$XCODEPROJ")" && TMPDIR="$TMP" xcodebuild -project "$XCODEPROJ" -target "$NAME" \
      -configuration Release -sdk iphoneos "SYMROOT=$WORK/build" "OBJROOT=$WORK/obj" \
      ONLY_ACTIVE_ARCH=NO CODE_SIGN_IDENTITY= CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO \
      DEVELOPMENT_TEAM= PROVISIONING_PROFILE_SPECIFIER= "MODULE_CACHE_DIR=$WORK/modules" \
      "CLANG_MODULE_CACHE_PATH=$WORK/modules" COMPILER_INDEX_STORE_ENABLE=NO build) \
      > "$WORK/xcodebuild.log" 2>&1
    BUNDLE="$(find "$WORK/build" -type d -name '*.app' -not -path '*/*.app/*' | head -1)"
    [ -n "$BUNDLE" ] || { echo "mobile-size: xcodebuild produced no application" >&2; exit 1; }
    find "$WORK/build" -name '*.dSYM' -prune -exec rm -rf {} +
    # The App Store strips symbols on delivery; -x is what Xcode's own
    # "Strip Linked Product" does to an application.
    xcrun strip -x -o "$WORK/stripped-executable" "$BUNDLE/$NAME"
    python3 "$HERE/benchutil.py" size --scratch "$TMP" "$BUNDLE" > "$WORK/bundle-size.json"
    python3 - "$WORK/bundle-size.json" "$BUNDLE/$NAME" "$WORK/stripped-executable" > "$WORK/ios-size.json" <<'PY'
import json, os, sys
size = json.load(open(sys.argv[1]))
size["what"] = "unsigned Release .app, arm64, generic iOS device"
size["executable_bytes"] = os.path.getsize(sys.argv[2])
size["executable_stripped_bytes"] = os.path.getsize(sys.argv[3])
json.dump(size, sys.stdout, indent=2, sort_keys=True)
PY
    ;;
  android)
    : "${ANDROID_HOME:?ANDROID_HOME must point at an Android SDK}"
    say "generating the Gradle project"
    generate android android-source "$WORK/android-source.log"
    GRADLEW="$(find "$WORK/app" -name gradlew -path '*android-source*' | head -1)"
    [ -n "$GRADLEW" ] || { echo "mobile-size: no Gradle project was generated" >&2; exit 1; }
    chmod +x "$GRADLEW"
    say "gradle assembleRelease"
    # Its own Gradle home, and no SDK downloads: a size run must not change the
    # machine it runs on.
    (cd "$(dirname "$GRADLEW")" && JAVA_HOME="$JAVA17_HOME" PATH="$JAVA17_HOME/bin:$PATH" \
      GRADLE_USER_HOME="${GRADLE_USER_HOME:-$WORK/gradle-home}" ANDROID_SDK_ROOT="$ANDROID_HOME" \
      ANDROID_USER_HOME="$WORK/android-user-home" TMPDIR="$TMP" \
      ./gradlew --no-daemon -Pandroid.builder.sdkDownload=false "-Djava.io.tmpdir=$TMP" \
      assembleRelease) > "$WORK/gradle.log" 2>&1
    APK="$(find "$(dirname "$GRADLEW")" -name '*.apk' -path '*release*' | head -1)"
    [ -n "$APK" ] || { echo "mobile-size: gradle produced no APK" >&2; exit 1; }
    python3 - "$APK" > "$WORK/android-size.json" <<'PY'
import json, os, sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as apk:
    entries = apk.infolist()
    dex = sum(e.file_size for e in entries if e.filename.endswith(".dex"))
    unpacked = sum(e.file_size for e in entries)
json.dump({"what": "release APK, R8 minified", "apk_bytes": os.path.getsize(sys.argv[1]),
           "unpacked_bytes": unpacked, "dex_bytes": dex, "files": len(entries)},
          sys.stdout, indent=2, sort_keys=True)
PY
    ;;
  *)
    echo "mobile-size: --target must be ios or android" >&2
    exit 2
    ;;
esac
say "wrote $WORK/$TARGET-size.json"
cat "$WORK/$TARGET-size.json"
echo
