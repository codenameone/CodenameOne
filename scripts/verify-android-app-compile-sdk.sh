#!/usr/bin/env bash
#
# Rebuild an already-generated Codename One Android project at a different API
# level, to prove the whole generated project still builds there.
#
# scripts/build-android-app.sh pins compileSdk and targetSdk so the screenshot
# baselines and the emulator leg stay reproducible, which means the suite only
# ever exercises that one level. Issue #5701 was exactly what that misses: API
# 37 removed FingerprintManager, the port named it, and every generated app
# failed :app:compileDebugJavaWithJavac in sources the developer never wrote --
# with our CI green, because our CI never compiled against 37.
#
# This re-patches the project the primary build already produced and assembles
# it again at the requested level. It reuses the generated sources, the
# resolved dependencies and the warm Gradle cache, so it costs one more
# assemble rather than another full build, and it covers what an offline javac
# comparison cannot: AAPT, the manifest merger, aar metadata and packaging.
#
#   scripts/verify-android-app-compile-sdk.sh <gradle-project-dir> [api-level]
#
# The API level defaults to 37. The project directory is the one
# build-android-app.sh reports as its gradle_project_dir output.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

log() { echo "[verify-compile-sdk] $*"; }

# Same workspace environment build-android-app.sh loads, and for the same
# reason: JAVA17_HOME is written there by setup-workspace.sh rather than
# exported into the job, so a step that does not source it has no JDK 17.
TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
ENV_FILE="${TMPDIR}/codenameone-tools/tools/env.sh"
if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

GRADLE_PROJECT_DIR="${1:-}"
API_LEVEL="${2:-37}"

if [ -z "$GRADLE_PROJECT_DIR" ]; then
  log "usage: $0 <gradle-project-dir> [api-level]" >&2
  exit 2
fi
if [ ! -f "$GRADLE_PROJECT_DIR/gradlew" ]; then
  log "not a generated Gradle project: $GRADLE_PROJECT_DIR" >&2
  exit 2
fi

# The NEWEST stable revision of the level, not its ".0". An API level is not
# one platform from 37 onward -- android-37.0, .1 and .2 are separate packages
# and a removal can land in any of them -- and the builder picks the newest
# revision a developer's SDK has, so pinning the .0 here would verify a
# platform no user compiles against and merge a regression introduced later in
# the level.
PLATFORM_PACKAGE=$("$SCRIPT_DIR/android/newest-platform-package.sh" "$API_LEVEL")
# The name AGP has to be given to reach exactly that platform: the bare level
# always resolves to the .0.
PLATFORM_NAME="${PLATFORM_PACKAGE#platforms;android-}"

# The sdkmanager INSIDE the SDK root we are about to build against, before the
# one on PATH. On a machine with the Homebrew android-commandlinetools cask the
# PATH copy belongs to a different SDK root entirely, so it installs the
# platform somewhere the build will never look -- and it reports that as a
# plain failure or, worse, as success. Measured here: /opt/homebrew/bin/
# sdkmanager exits 1 for a package the SDK-root copy installs fine.
SDKMANAGER=""
if [ -x "${ANDROID_SDK_ROOT:-}/cmdline-tools/latest/bin/sdkmanager" ]; then
  SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
elif [ -x "${ANDROID_HOME:-}/cmdline-tools/latest/bin/sdkmanager" ]; then
  SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
elif command -v sdkmanager >/dev/null 2>&1; then
  SDKMANAGER="sdkmanager"
fi

if [ -z "$SDKMANAGER" ]; then
  log "no sdkmanager on PATH or under ANDROID_SDK_ROOT/ANDROID_HOME" >&2
  exit 1
fi

# sdkmanager reads a handful of license prompts off stdin and then stops
# reading, which kills `yes` with SIGPIPE -- exit 141, and under `pipefail`
# that is the status of the whole pipeline even though sdkmanager itself
# succeeded. Read sdkmanager's own status out of PIPESTATUS instead, so the
# check below tests the install rather than the writer at the other end.
sdk_install() {
  local status
  set +o pipefail
  # JDK 17, not the workspace default. env.sh above sets JAVA_HOME to the JDK 8
  # the framework is built with, and sdkmanager refuses to run on it -- "This
  # tool requires JDK 17 or later", exit 1, which reads as "the package could
  # not be installed" and has nothing to do with the package.
  yes | JAVA_HOME="${JDK_HOME:-$JAVA17_HOME}" "$SDKMANAGER" "$@" >/dev/null
  status=${PIPESTATUS[1]}
  set -o pipefail
  return "$status"
}

log "Installing $PLATFORM_PACKAGE"
# Not tolerated the way build-android-app.sh tolerates it. There the platform
# is usually preinstalled on the runner and a failed install still leaves a
# working build; here the whole point is to compile against a platform that is
# NOT preinstalled, so a failed install would otherwise leave Gradle to fall
# back and quietly verify the level we already tested.
if ! sdk_install "$PLATFORM_PACKAGE"; then
  log "failed to install $PLATFORM_PACKAGE" >&2
  exit 1
fi
sdk_install --licenses || true

PATCH_GRADLE_SOURCE_PATH="$SCRIPT_DIR/android/lib"
PATCH_GRADLE_JAVA="${JDK_HOME:-${JAVA17_HOME:-}}/bin/java"
if [ ! -x "$PATCH_GRADLE_JAVA" ]; then
  log "JDK java binary missing at $PATCH_GRADLE_JAVA (set JAVA17_HOME)" >&2
  exit 1
fi

PATCH_GRADLE_MODULES=(--app "$GRADLE_PROJECT_DIR/app/build.gradle")
if [ -f "$GRADLE_PROJECT_DIR/wear/build.gradle" ]; then
  PATCH_GRADLE_MODULES+=(--app "$GRADLE_PROJECT_DIR/wear/build.gradle")
fi

# compileSdk names the exact platform installed above; targetSdk stays the
# integer level, because that is an API level in the manifest and has no minor.
log "Re-pinning $GRADLE_PROJECT_DIR to compileSdk $PLATFORM_NAME, targetSdk $API_LEVEL"
"$PATCH_GRADLE_JAVA" "$PATCH_GRADLE_SOURCE_PATH/PatchGradleFiles.java" \
  --root "$GRADLE_PROJECT_DIR/build.gradle" \
  "${PATCH_GRADLE_MODULES[@]}" \
  --compile-sdk "$PLATFORM_NAME" \
  --target-sdk "$API_LEVEL"

# AGP compares this against the name of the platform it RESOLVED, and from API
# 37 that name carries a minor -- compileSdk 37 resolves to android-37.0 and
# AGP asks to be suppressed with "37.0", so the bare number silences nothing.
# The property is a comma-separated list, so both spellings go in.
GRADLE_PROPS="$GRADLE_PROJECT_DIR/gradle.properties"
if [ -f "$GRADLE_PROPS" ]; then
  grep -v '^android.suppressUnsupportedCompileSdk=' "$GRADLE_PROPS" > "$GRADLE_PROPS.tmp" || true
  mv "$GRADLE_PROPS.tmp" "$GRADLE_PROPS"
fi
echo "android.suppressUnsupportedCompileSdk=$API_LEVEL,$API_LEVEL.0,$PLATFORM_NAME" >> "$GRADLE_PROPS"

log "Assembling at API $API_LEVEL"
ORIGINAL_JAVA_HOME="${JAVA_HOME:-}"
export JAVA_HOME="${JDK_HOME:-$JAVA17_HOME}"
(
  cd "$GRADLE_PROJECT_DIR"
  chmod +x ./gradlew
  ./gradlew --no-daemon --stacktrace assembleDebug
)
export JAVA_HOME="$ORIGINAL_JAVA_HOME"

log "OK: the generated project builds at compileSdk $PLATFORM_NAME, targetSdk $API_LEVEL"
