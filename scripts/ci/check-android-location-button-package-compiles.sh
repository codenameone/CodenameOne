#!/usr/bin/env bash
#
# Compiles the location-button package the way an application build does.
#
# That package is deliberately excluded from the Android port jar: it references
# androidx.core.locationbutton, which is only on the classpath of applications that reference
# com.codename1.location.LocationButton, and the builder deletes it for everyone else. The
# consequence is that no ordinary build of this repository ever compiles it, so nothing local
# notices when a name in it stops resolving.
#
# That matters more here than for a package whose dependency is stable. androidx.core.locationbutton
# is at 1.0.0-alpha01 and the guide that introduces it says so; a method it renames turns this
# package into something the application build fails on, forty minutes into a device job.
#
# Compiled against the REAL library rather than stubs, which is the difference between this check
# and check-android-cipher-package-compiles.sh. Stubs here would be written from the same reading of
# the API as the code under test, so a wrong method name would appear in both and the check would
# pass on a package that cannot compile -- a gate satisfiable by the mistake it exists to catch.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

fail() {
    echo "check-android-location-button: $*" >&2
    exit 1
}

# JDK 11 or newer, and that is the library rather than a preference: its class files are Java 11
# bytecode (major version 55), and a JDK 8 javac refuses them outright with "class file has wrong
# version 55.0, should be 52.0". The generated application project is compiled with JAVA17_HOME for
# the same reason -- see the android.useGradle8 check in AndroidGradleBuilder -- so this uses the
# same JDK the real build does. Still -source 8 -target 8 against android.jar, because that is what
# the port is written to.
pick_javac() {
    for home in "${JAVA17_HOME:-}" "${JAVA_HOME:-}"; do
        [ -n "$home" ] || continue
        [ -x "$home/bin/javac" ] || continue
        if "$home/bin/javac" -version 2>&1 | grep -E 'javac 1\.[0-8]' >/dev/null; then
            continue
        fi
        echo "$home/bin/javac"
        return 0
    done
    if command -v javac >/dev/null 2>&1 \
            && ! javac -version 2>&1 | grep -E 'javac 1\.[0-8]' >/dev/null; then
        echo javac
        return 0
    fi
    return 1
}
JAVAC="$(pick_javac || true)"
[ -n "$JAVAC" ] || fail "no JDK 11 or newer javac found. The location button library ships Java 11
  class files, which a Java 8 javac cannot read. Set JAVA17_HOME (the Android port build already
  needs it) or put a newer JDK on PATH."

# The version the builder injects. Read out of the builder rather than repeated here, so a bump
# there is what this check follows -- a hardcoded second copy would go on testing the old one.
VERSION="$(sed -n 's/.*"android.locationButton.version", *"\([^"]*\)".*/\1/p' \
    "$REPO_ROOT/maven/codenameone-maven-plugin/src/main/java/com/codename1/builders/AndroidGradleBuilder.java" \
    | head -1)"
[ -n "$VERSION" ] || fail "could not read the location button library version out of
  AndroidGradleBuilder. If the android.locationButton.version default moved, this check has to
  follow it."

# An API 37 platform first, and that ordering is the point of this check rather than a
# convenience: the location button exists BECAUSE of Android 17, and the platform types the
# library talks to (android.app.permissionui.*) arrived there. Compiling against the older
# android.jar the port itself uses would type-check the package against a platform that has never
# heard of the feature, which is a weaker answer than it looks.
#
# cn1-binaries is the fallback, because a working copy without an Android SDK should still get the
# rest of the value -- cross-package visibility, signature drift against the library, ordinary
# syntax errors. CI passes --require-api-37 so that fallback cannot quietly become the answer
# there.
REQUIRE_API_37=0
for arg in "$@"; do
    case "$arg" in
        --require-api-37) REQUIRE_API_37=1 ;;
        *) fail "unknown argument: $arg" ;;
    esac
done

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
# Android 17 ships as minor-versioned platform directories (android-37.0, android-37.1, ...), which
# is new: every platform before it was a bare integer. A glob over 37.* rather than a fixed name,
# and the highest one, so a machine that has moved on to a later minor release is not told to
# install one it has superseded.
# `|| true` on the listing, and the "/android.jar" appended only once a directory was actually
# found: under `set -e` an assignment whose command substitution fails takes the whole script with
# it, so a machine with no Android SDK exited here rather than reaching the fallback below.
API_37_PLATFORM="$(ls -d "$SDK_ROOT"/platforms/android-37* 2>/dev/null | sort -V | tail -1 || true)"
ANDROID_JAR=""
if [ -n "$API_37_PLATFORM" ] && [ -f "$API_37_PLATFORM/android.jar" ]; then
    ANDROID_JAR="$API_37_PLATFORM/android.jar"
fi
CN1_BINARIES="${CN1_BINARIES:-$REPO_ROOT/../cn1-binaries}"
if [ -z "$ANDROID_JAR" ]; then
    if [ "$REQUIRE_API_37" = "1" ]; then
        fail "no API 37 platform found under $SDK_ROOT/platforms.
  Install one with: sdkmanager --sdk_root=\"\$ANDROID_HOME\" 'platforms;android-37.0'
  (--require-api-37 was passed, so falling back to the cn1-binaries android.jar would report a
  weaker check as if it were this one.)"
    fi
    if [ -f "$CN1_BINARIES/android/android.jar" ]; then
        echo "check-android-location-button: no API 37 platform installed; falling back to the" >&2
        echo "cn1-binaries android.jar. The library resolves either way, but this does not check" >&2
        echo "the package against the platform the feature belongs to." >&2
        ANDROID_JAR="$CN1_BINARIES/android/android.jar"
    fi
fi
[ -n "$ANDROID_JAR" ] || fail "no android.jar found. Expected an API 37 platform under
  $SDK_ROOT/platforms, or \$CN1_BINARIES/android/android.jar (currently $CN1_BINARIES)."

PACKAGE_DIR="$REPO_ROOT/Ports/Android/src/com/codename1/impl/android/locationbutton"
[ -d "$PACKAGE_DIR" ] || fail "the location button package is missing from $PACKAGE_DIR"

# The real AAR, from the repository the generated gradle project resolves it from. A cache
# directory can be pointed at with CN1_LOCATION_BUTTON_AAR so a CI run or an offline working copy
# does not have to reach the network.
AAR="${CN1_LOCATION_BUTTON_AAR:-}"
if [ -z "$AAR" ]; then
    AAR="$WORK_DIR/locationbutton-$VERSION.aar"
    URL="https://dl.google.com/dl/android/maven2/androidx/core/locationbutton/locationbutton/$VERSION/locationbutton-$VERSION.aar"
    curl -fsSL -o "$AAR" "$URL" || fail "could not download $URL
  Point CN1_LOCATION_BUTTON_AAR at a local copy of the aar to run this check offline."
fi
[ -f "$AAR" ] || fail "no location button aar at $AAR"

mkdir -p "$WORK_DIR/aar"
unzip -o -q "$AAR" -d "$WORK_DIR/aar" || fail "$AAR is not readable as an aar"
LIBRARY_JAR="$WORK_DIR/aar/classes.jar"
[ -f "$LIBRARY_JAR" ] || fail "$AAR has no classes.jar"
# Listed to a file rather than piped into grep -q. Under `set -o pipefail` a `grep -q` that matches
# exits at the first hit, unzip is killed by SIGPIPE, and the pipeline reports failure -- so the
# check failed exactly when the class it looks for WAS there.
unzip -l "$LIBRARY_JAR" > "$WORK_DIR/library-entries.txt"
grep -q 'androidx/core/locationbutton/LocationButton.class' "$WORK_DIR/library-entries.txt" \
    || fail "$AAR does not carry androidx.core.locationbutton.LocationButton. The artifact
  coordinates changed, and this check would otherwise pass by compiling against nothing."

# Compiled port classes, not sources, for the reason the cipher check gives: the port needs a
# support-library classpath this check has no business assembling, and compiled classes carry the
# exact member visibility the application build links against.
CORE_CLASSES="$REPO_ROOT/maven/core/target/classes"
if [ ! -f "$CORE_CLASSES/com/codename1/location/LocationButton.class" ]; then
    CORE_CLASSES="$(find "${CN1_LOCAL_REPO:-/tmp/cn1-local-repo}" "$HOME/.m2/repository" \
        -path '*com/codenameone/codenameone-core/*' -name 'codenameone-core-*.jar' \
        ! -name '*sources*' ! -name '*javadoc*' 2>/dev/null | sort | tail -1 || true)"
fi
[ -n "$CORE_CLASSES" ] || fail "no compiled core found. Expected
  \$REPO_ROOT/maven/core/target/classes, or a codenameone-core jar in a local repository."

PORT_CLASSES="$REPO_ROOT/maven/android/target/classes"
[ -d "$PORT_CLASSES" ] || fail "no compiled Android port at $PORT_CLASSES; run
  mvn -f maven/pom.xml -Pcompile-android -pl android -am -DskipTests compile"
[ -f "$PORT_CLASSES/com/codename1/impl/android/AndroidImplementation.class" ] \
    || fail "$PORT_CLASSES has no AndroidImplementation; rebuild the Android port"

# The build copies the package's .java files into the output as resources, for the builder to stage
# into the generated application - that is expected. What must not be there is compiled output: the
# module excludes the package from compilation, which is the whole reason nothing here type-checks
# it and this script exists.
PACKAGE_CLASSES="$PORT_CLASSES/com/codename1/impl/android/locationbutton"
if [ -d "$PACKAGE_CLASSES" ] && [ -n "$(find "$PACKAGE_CLASSES" -name '*.class' 2>/dev/null)" ]; then
    fail "the location button package is being compiled into the port output. It is meant to be
  excluded so the builder can delete it for applications that never show the button; if that
  changed, this check and the deletable-package arrangement both need revisiting."
fi

OUT="$WORK_DIR/classes"
mkdir -p "$OUT"

set +e
"$JAVAC" -nowarn -proc:none -d "$OUT" \
    -source 8 -target 8 \
    -bootclasspath "$ANDROID_JAR" \
    -cp "$ANDROID_JAR:$LIBRARY_JAR:$PORT_CLASSES:$CORE_CLASSES" \
    "$PACKAGE_DIR"/*.java 2>"$WORK_DIR/javac.log"
STATUS=$?
set -e

if [ $STATUS -ne 0 ]; then
    echo "check-android-location-button: the location button package does not compile against" >&2
    echo "androidx.core.locationbutton:$VERSION." >&2
    echo "This package is excluded from the port jar, so nothing else in a local build compiles" >&2
    echo "it -- an application build would be the next thing to notice." >&2
    echo >&2
    grep -E "error:" "$WORK_DIR/javac.log" | head -20 >&2
    exit 1
fi

echo "check-android-location-button: the package compiles against androidx.core.locationbutton:$VERSION"
echo "  using $ANDROID_JAR"
