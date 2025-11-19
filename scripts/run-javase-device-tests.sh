#!/usr/bin/env bash
# Run Codename One device-runner tests on the Java SE (desktop) target
set -euo pipefail

jd_log() { echo "[run-javase-device-tests] $1"; }
ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/android/tests"
if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/Cn1ssChunkTools.java" ]; then
  jd_log "CN1SS helper sources not found at $CN1SS_HELPER_SOURCE_DIR" >&2
  exit 2
fi

source "$SCRIPT_DIR/lib/cn1ss.sh"
cn1ss_log() { jd_log "$1"; }

if [ -z "${JAVA_BIN:-}" ]; then
  if [ -n "${JAVA_HOME_17_X64:-}" ] && [ -x "$JAVA_HOME_17_X64/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME_17_X64/bin/java"
  elif [ -n "${JAVA_HOME_17:-}" ] && [ -x "$JAVA_HOME_17/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME_17/bin/java"
  else
    JAVA_BIN="$(command -v java || true)"
  fi
fi

if [ -z "$JAVA_BIN" ]; then
  jd_log "java binary not found on PATH" >&2
  exit 2
fi

JAVA_VERSION_RAW="$($JAVA_BIN -version 2>&1 | head -n1 | sed -E 's/.*version \"([^\"]+)\".*/\1/' || true)"
JAVA_VERSION_MAJOR="${JAVA_VERSION_RAW%%.*}"
if [ "$JAVA_VERSION_MAJOR" = "1" ]; then
  JAVA_VERSION_MAJOR="$(echo "$JAVA_VERSION_RAW" | cut -d. -f2)"
fi

if [ -z "$JAVA_VERSION_MAJOR" ] || [ "$JAVA_VERSION_MAJOR" -lt 17 ]; then
  jd_log "Java 17 or newer is required for CN1SS helpers (detected: ${JAVA_VERSION_RAW:-unknown})" >&2
  exit 2
fi

JAVAC_BIN="${JAVAC_BIN:-${JAVA_BIN%/*}/javac}"
if [ -z "$JAVAC_BIN" ] || [ ! -x "$JAVAC_BIN" ]; then
  JAVAC_BIN="$(command -v javac || true)"
fi
if [ -z "$JAVAC_BIN" ] || [ ! -x "$JAVAC_BIN" ]; then
  jd_log "javac binary not found" >&2
  exit 2
fi

JAVAC_VERSION_RAW="$($JAVAC_BIN -version 2>&1 | head -n1 | sed -E 's/.* ([0-9]+(\.[0-9]+)*).*/\1/' || true)"
JAVAC_VERSION_MAJOR="${JAVAC_VERSION_RAW%%.*}"
if [ "$JAVAC_VERSION_MAJOR" = "1" ]; then
  JAVAC_VERSION_MAJOR="$(echo "$JAVAC_VERSION_RAW" | cut -d. -f2)"
fi

if [ -z "$JAVAC_VERSION_MAJOR" ] || [ "$JAVAC_VERSION_MAJOR" -lt 17 ]; then
  jd_log "Java 17 or newer is required for compilation (javac detected: ${JAVAC_VERSION_RAW:-unknown})" >&2
  exit 2
fi

CN1SS_JAVAC_BIN="$JAVAC_BIN"

cn1ss_setup "$JAVA_BIN" "$CN1SS_HELPER_SOURCE_DIR"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts/desktop-device-runner}"
ensure_dir "$ARTIFACTS_DIR"
LOG_FILE="$ARTIFACTS_DIR/javase-device-runner.log"
SCREENSHOT_DIR="$ARTIFACTS_DIR/screenshots"
PREVIEW_DIR="$ARTIFACTS_DIR/previews"
ensure_dir "$SCREENSHOT_DIR"
ensure_dir "$PREVIEW_DIR"

jd_log "Ensuring Java SE port is built"
ant -noinput -buildfile Ports/JavaSE/build.xml jar

CN1_CLASSPATH="CodenameOne/dist/CodenameOne.jar:Ports/JavaSE/dist/JavaSE.jar:Ports/CLDC11/dist/CLDC11.jar"
if [ -d "Ports/JavaSE/dist/lib" ]; then
  CN1_CLASSPATH+="$(printf ':%s' Ports/JavaSE/dist/lib/*)"
fi

BUILD_DIR="$(mktemp -d "${TMPDIR:-/tmp}/cn1-javase-tests-XXXXXX" 2>/dev/null || echo "${TMPDIR:-/tmp}/cn1-javase-tests")"
SRC_ROOT="$SCRIPT_DIR/device-runner-app"
MAIN_SRC="$SRC_ROOT/main"
TEST_SRC="$SRC_ROOT/tests"

if [ ! -d "$MAIN_SRC" ] || [ ! -d "$TEST_SRC" ]; then
  jd_log "Device runner sources missing under $SRC_ROOT" >&2
  exit 2
fi

mkdir -p "$BUILD_DIR/src"
rsync -a "$MAIN_SRC/" "$BUILD_DIR/src/"
rsync -a "$TEST_SRC/" "$BUILD_DIR/src/"

jd_log "Compiling device-runner application sources"
find "$BUILD_DIR/src" -name '*.java' -print0 | xargs -0 "$JAVAC_BIN" -cp "$CN1_CLASSPATH" -d "$BUILD_DIR/classes"

JAVA_CMD=(timeout --foreground --kill-after=30s 7m xvfb-run -a "$JAVA_BIN" \
  -cp "$CN1_CLASSPATH:$BUILD_DIR/classes" \
  com.codename1.impl.javase.Simulator com.codenameone.examples.hellocodenameone.HelloCodenameOne)

jd_log "Launching Java SE simulator for device-runner app"
set +e
"${JAVA_CMD[@]}" >"$LOG_FILE" 2>&1
rc=$?
set -e

if [ $rc -ne 0 ]; then
  jd_log "Simulator exited with status $rc (see log at $LOG_FILE)"
fi

SIM_EXIT_CODE=$rc

TEST_NAMES_RAW="$(cn1ss_list_tests "$LOG_FILE" 2>/dev/null | awk 'NF' | sort -u || true)"
declare -a TEST_NAMES=()
if [ -n "$TEST_NAMES_RAW" ]; then
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAMES+=("$name")
  done <<< "$TEST_NAMES_RAW"
else
  TEST_NAMES+=("default")
fi

jd_log "Detected CN1SS test streams: ${TEST_NAMES[*]}"

declare -a SOURCES=("LOG:$LOG_FILE")

for test in "${TEST_NAMES[@]}"; do
  png_dest="$SCREENSHOT_DIR/${test}.png"
  if cn1ss_decode_test_png "$test" "$png_dest" "${SOURCES[@]}"; then
    jd_log "Decoded screenshot for '$test' -> $png_dest"
  else
    jd_log "No screenshot payload detected for '$test'"
    rm -f "$png_dest" 2>/dev/null || true
  fi

  preview_dest="$PREVIEW_DIR/${test}.jpg"
  if cn1ss_decode_test_preview "$test" "$preview_dest" "${SOURCES[@]}"; then
    jd_log "Decoded preview for '$test' -> $preview_dest"
  else
    rm -f "$preview_dest" 2>/dev/null || true
  fi

done

# Emit a simple summary for debugging
SUMMARY_FILE="$ARTIFACTS_DIR/summary.txt"
{
  echo "Simulator exit code: $rc"
  echo "Log file: $LOG_FILE"
  echo "Screenshots:"
  find "$SCREENSHOT_DIR" -maxdepth 1 -type f -name '*.png' -printf '  - %f\n' 2>/dev/null || true
} > "$SUMMARY_FILE"

jd_log "Desktop device-runner artifacts stored in $ARTIFACTS_DIR"

exit $SIM_EXIT_CODE
