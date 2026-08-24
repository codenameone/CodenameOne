#!/usr/bin/env bash
# Builds the device runtime app for an Android emulator or device, installs it,
# and pushes a program to it.
#
# Android needs no special translator mode: it has reflection, so the linker is
# registered on every build and the shims are ordinary compiled classes. That
# asymmetry with iOS is the whole reason both platforms are tested.
#
# Usage: run-device-runtime-android.sh [--skip-build] [program.java]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP="$ROOT/scripts/cn1-device-runtime"
WORK="${CN1_DEVRUNTIME_WORK:-${TMPDIR:-/tmp}/cn1-devruntime-android}"
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

# adb picks a device implicitly only when exactly one is attached, and it will
# happily choose a Wear emulator over the phone. Pin the serial.
SERIAL="${CN1_ADB_SERIAL:-$(adb devices | awk '/\tdevice$/ {print $1; exit}')}"
if [ -z "$SERIAL" ]; then
    echo "no Android device or emulator attached" >&2
    exit 1
fi
echo "device: $SERIAL"
ADB=(adb -s "$SERIAL")

MVN_ARGS=()
[ -n "${SETTINGS_LOCAL:-}" ] && MVN_ARGS+=(-s "$SETTINGS_LOCAL")
[ -n "${M2_LOCAL:-}" ] && MVN_ARGS+=(-Dmaven.repo.local="$M2_LOCAL")

if [ "$SKIP_BUILD" = 0 ]; then
    # Same trap the iOS script guards: the app resolves the framework from the
    # local repository, so an edit to com.codename1.impl.interp is invisible here
    # until core is installed, and the Android port jar carries its own copy of
    # those classes on top of that. Rebuilding both costs a minute and removes
    # a failure that reads as "my change did nothing".
    echo "=== refreshing core and the Android port ==="
    for module in core android; do
        (cd "$ROOT/maven" && mvn -q ${MVN_ARGS[@]+"${MVN_ARGS[@]}"} \
            -Pcompile-android -f "$module/pom.xml" install -DskipTests) || {
            echo "could not rebuild $module" >&2
            exit 1
        }
    done

    echo "=== building ==="
    # The generated Gradle project is copied into, not regenerated: a framework
    # jar that changed since the last run keeps its old copy there and the app
    # silently runs yesterday's core. That failure reads as a protocol bug --
    # the device rejecting a bundle the current writer plainly produced.
    #
    # target/classes goes too. The build copies .java files there as resources
    # so the generated Gradle project can carry them, and a class deleted from
    # src/ keeps its copy in target/classes indefinitely -- which is how a file
    # that no longer exists ends up failing the compile.
    rm -rf "$APP/android/target/codenameone" \
           "$APP/android/target/classes" \
           "$APP"/android/target/*-android-source
    (cd "$APP" && JAVA_HOME="${JAVA17_HOME:-$JAVA_HOME}" \
        PATH="${JAVA17_HOME:-$JAVA_HOME}/bin:$PATH" \
        mvn -o ${MVN_ARGS[@]+"${MVN_ARGS[@]}"} package -DskipTests \
        -Dcodename1.platform=android \
        -Dcodename1.buildTarget=android-source \
        -Dcodename1.arg.android.xapplication_attr='android:usesCleartextTraffic="true"' \
        -Dmaven.compiler.fork=true \
        -Dmaven.compiler.executable="${JAVA17_HOME:-$JAVA_HOME}/bin/javac" \
        -Dopen=false 2>&1 | tee "$WORK/build.log" | grep -E "BUILD|ERROR" | head -20)
fi

GRADLE_DIR="$(find "$APP/android/target" -maxdepth 1 -name '*-android-source' -type d | head -1)"
[ -n "$GRADLE_DIR" ] || { echo "no gradle project generated; see $WORK/build.log" >&2; exit 1; }

if [ "$SKIP_BUILD" = 0 ]; then
    # android-source stops at generating the Gradle project; the cloud build
    # server is what normally compiles it. Locally that step is ours.
    #
    # The retarget is a workaround for a malformed local SDK package: the
    # installed android-37.0 declares AndroidVersion.ApiLevel=37.0 with
    # Platform.Version=17, which AGP rejects. 36 is the newest coherent one
    # here. Drop this once the SDK package is fixed.
    # BSD (macOS) sed requires an explicit backup suffix argument -- `-i ''`
    # for none -- while GNU sed (Linux) takes `-i` alone and treats a
    # separate `''` as an input filename, aborting with "can't read : No
    # such file or directory" under `set -e`. Detect once, invoke through
    # the array to survive both shells.
    if sed --version >/dev/null 2>&1; then
        SED_INPLACE=(sed -i)
    else
        SED_INPLACE=(sed -i '')
    fi
    API="${CN1_ANDROID_API:-36}"
    "${SED_INPLACE[@]}" -e "s/compileSdkVersion 37/compileSdkVersion $API/" \
                        -e "s/targetSdkVersion 37/targetSdkVersion $API/" \
                        "$GRADLE_DIR/app/build.gradle"
    # One ABI for a sideloadable APK. The runtime carries ML Kit, CameraX and
    # ARCore on purpose -- they are the reason to debug on a device rather than
    # in the simulator -- and ML Kit's bundled models are ~287MB of native
    # libraries across four ABIs, which makes a universal APK 323MB. arm64 alone
    # is 110MB, and arm64 is every device worth testing on and the only emulator
    # image that runs at speed on an Apple-silicon Mac.
    #
    # The store build does not do this: it ships an app bundle, and Play
    # delivers one ABI per device by itself.
    ABI="${CN1_ANDROID_ABI:-arm64-v8a}"
    if ! grep -q abiFilters "$GRADLE_DIR/app/build.gradle"; then
        "${SED_INPLACE[@]}" -e "s/    defaultConfig {/    defaultConfig {\
        ndk { abiFilters '$ABI' }/" "$GRADLE_DIR/app/build.gradle"
    fi
    # The generated project has no local.properties -- the cloud build server
    # supplies the SDK location. Locally it has to be written.
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
    [ -d "$SDK" ] || { echo "no Android SDK at $SDK; set ANDROID_HOME" >&2; exit 1; }
    echo "sdk.dir=$SDK" > "$GRADLE_DIR/local.properties"

    echo "=== gradle assembleDebug (api $API) ==="
    (cd "$GRADLE_DIR" && JAVA_HOME="${JAVA17_HOME:-$JAVA_HOME}" ANDROID_HOME="$SDK" \
        ./gradlew --no-daemon assembleDebug > "$WORK/gradle.log" 2>&1) || {
        echo "gradle failed; last errors:" >&2
        grep -E "error:|FAILURE|What went wrong" -A3 "$WORK/gradle.log" | head -30 >&2
        exit 1
    }
fi

APK="$(find "$APP/android/target" -name '*.apk' -print 2>/dev/null | head -1 || true)"
[ -n "$APK" ] || { echo "no apk produced; see $WORK/gradle.log" >&2; exit 1; }

echo "=== installing $APK ==="
"${ADB[@]}" install -r "$APK" >/dev/null

echo "=== launching ==="
"${ADB[@]}" logcat -c
# `monkey` sends a LAUNCH_SINGLE_TOP intent, which a just-replaced install can
# answer with "already running" against a window that is on its way out -- the
# app never actually starts and nothing says so. am start is explicit.
"${ADB[@]}" shell am force-stop com.codenameone.devruntime >/dev/null 2>&1 || true
"${ADB[@]}" shell am start -n \
    com.codenameone.devruntime/.DeviceRuntimeAppStub >/dev/null 2>&1

for _ in $(seq 1 40); do
    if "${ADB[@]}" logcat -d | grep -q "CN1SS:DEVRUNTIME"; then
        break
    fi
    sleep 1
done
"${ADB[@]}" logcat -d | grep "CN1SS:DEVRUNTIME" | tail -3 || {
    echo "the app never reported its device runtime status; last crash output:" >&2
    "${ADB[@]}" logcat -d | grep -E "AndroidRuntime|FATAL|System.err" | tail -20 >&2
    exit 1
}

if [ -n "$PROGRAM" ]; then
    # The device dials out and the desktop listens, so this is `adb reverse`:
    # it maps the *device's* 127.0.0.1:18234 onto the host's. `adb forward` is
    # the opposite direction and was what the old listening design needed.
    "${ADB[@]}" reverse --remove tcp:18234 >/dev/null 2>&1 || true
    "${ADB[@]}" reverse tcp:18234 tcp:18234 >/dev/null
    echo "=== pushing $PROGRAM ==="
    "$ROOT/scripts/cn1-push.sh" "$PROGRAM" 18234
    sleep 2
    "${ADB[@]}" logcat -d | grep -E "CN1SS:|devruntime" | tail -10 || true
fi
