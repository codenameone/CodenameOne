#!/usr/bin/env bash
# Run JavaSE simulator integration screenshot tests in both single and multi-window modes.
set -euo pipefail

js_log() { echo "[run-javase-simulator-integration-tests] $1"; }
ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

source "$SCRIPT_DIR/lib/cn1ss.sh"
cn1ss_log() { js_log "$1"; }

if [ -n "${JAVA_HOME_17_X64:-}" ] && [ -x "$JAVA_HOME_17_X64/bin/java" ]; then
  JAVA_BIN="$JAVA_HOME_17_X64/bin/java"
elif [ -n "${JAVA_HOME_17:-}" ] && [ -x "$JAVA_HOME_17/bin/java" ]; then
  JAVA_BIN="$JAVA_HOME_17/bin/java"
else
  JAVA_BIN="$(command -v java || true)"
fi

if [ -z "$JAVA_BIN" ]; then
  js_log "java binary not found" >&2
  exit 2
fi

JAVAC_BIN="${JAVAC_BIN:-${JAVA_BIN%/*}/javac}"
if [ ! -x "$JAVAC_BIN" ]; then
  JAVAC_BIN="$(command -v javac || true)"
fi
if [ -z "$JAVAC_BIN" ] || [ ! -x "$JAVAC_BIN" ]; then
  js_log "javac binary not found" >&2
  exit 2
fi

detect_java8_home() {
  local candidate="" version_raw="" major=""
  for candidate in \
    "${JAVA_HOME:-}" \
    "${JAVA8_HOME:-}" \
    "${JAVA_HOME_8_X64:-}" \
    "${JAVA_HOME_8:-}" \
    "/usr/lib/jvm/java-8-openjdk-amd64" \
    "/usr/lib/jvm/temurin-8-jdk-amd64" \
    "/usr/lib/jvm/zulu8-ca-amd64"; do
    [ -n "$candidate" ] || continue
    [ -x "$candidate/bin/javac" ] || continue
    version_raw="$("$candidate/bin/javac" -version 2>&1 | head -n1 | sed -E 's/.* ([0-9]+(\.[0-9]+)*).*/\1/' || true)"
    major="${version_raw%%.*}"
    if [ "$major" = "1" ]; then
      major="$(echo "$version_raw" | cut -d. -f2)"
    fi
    if [ "$major" = "8" ]; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}

JAVA8_HOME_DETECTED="$(detect_java8_home || true)"
if [ -z "$JAVA8_HOME_DETECTED" ]; then
  js_log "Unable to locate a Java 8 JDK for ant build (required for source/target 6 modules)." >&2
  exit 2
fi

cn1ss_setup "$JAVA_BIN" "$SCRIPT_DIR/common/java"

ARTIFACTS_BASE="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
ARTIFACTS_DIR="${ARTIFACTS_BASE%/}/javase-simulator-tests"
RAW_DIR="$ARTIFACTS_DIR/raw"
PREVIEW_DIR="$ARTIFACTS_DIR/previews"
ensure_dir "$ARTIFACTS_DIR"
ensure_dir "$RAW_DIR"
ensure_dir "$PREVIEW_DIR"

SCREENSHOT_REF_DIR="$SCRIPT_DIR/javase/screenshots"
ensure_dir "$SCREENSHOT_REF_DIR"

SKIN_CACHE_DIR="${TMPDIR:-/tmp}/cn1-javase-skins"
ensure_dir "$SKIN_CACHE_DIR"
SIM_SKIN_PATH="${CN1_JAVASE_SKIN:-}"
if [ -z "$SIM_SKIN_PATH" ] || [ ! -f "$SIM_SKIN_PATH" ]; then
  js_log "Resolving simulator skin from codenameone-skins release"
  SKIN_ARCHIVE="$SKIN_CACHE_DIR/skins.tar.gz"
  SKIN_EXTRACT_DIR="$SKIN_CACHE_DIR/extracted"
  if [ ! -s "$SKIN_ARCHIVE" ]; then
    SKIN_URL="$(python3 - <<'PY'
import json
import urllib.request

with urllib.request.urlopen('https://api.github.com/repos/codenameone/codenameone-skins/releases/latest') as response:
    data = json.load(response)

assets = data.get('assets') or []
if not assets:
    raise SystemExit('')

print(assets[0].get('browser_download_url', ''))
PY
)"
    if [ -z "$SKIN_URL" ]; then
      js_log "Failed to resolve codenameone-skins release asset URL" >&2
      exit 2
    fi
    curl -fL --retry 3 --retry-delay 2 -o "$SKIN_ARCHIVE" "$SKIN_URL"
  fi
  if [ ! -d "$SKIN_EXTRACT_DIR" ]; then
    mkdir -p "$SKIN_EXTRACT_DIR"
    tar -xzf "$SKIN_ARCHIVE" -C "$SKIN_EXTRACT_DIR"
  fi
  PREFERRED_SKIN_NAME="${CN1_JAVASE_SKIN_NAME:-Nexus5X.skin}"
  SIM_SKIN_PATH="$(find "$SKIN_EXTRACT_DIR" -type f -name "$PREFERRED_SKIN_NAME" | head -n 1 || true)"
  if [ -z "$SIM_SKIN_PATH" ]; then
    SIM_SKIN_PATH="$(find "$SKIN_EXTRACT_DIR" -type f -name '*.skin' | head -n 1 || true)"
  fi
fi
if [ -z "$SIM_SKIN_PATH" ] || [ ! -f "$SIM_SKIN_PATH" ]; then
  js_log "Unable to locate a simulator skin file for JavaSE integration tests" >&2
  exit 2
fi
js_log "Using simulator skin: $SIM_SKIN_PATH"

js_log "Ensuring CLDC11 port is built (required bootclasspath for CodenameOne core)"
js_log "Using Java 8 for ant build: $JAVA8_HOME_DETECTED"
JAVA_HOME="$JAVA8_HOME_DETECTED" PATH="$JAVA8_HOME_DETECTED/bin:$PATH" ant -noinput -buildfile Ports/CLDC11/build.xml jar

js_log "Ensuring JavaSE port is built"
js_log "Using Java 8 for ant build: $JAVA8_HOME_DETECTED"
JAVA_HOME="$JAVA8_HOME_DETECTED" PATH="$JAVA8_HOME_DETECTED/bin:$PATH" ant -noinput -buildfile Ports/JavaSE/build.xml jar

CN1_CLASSPATH="$REPO_ROOT/CodenameOne/dist/CodenameOne.jar:$REPO_ROOT/Ports/JavaSE/dist/JavaSE.jar:$REPO_ROOT/Ports/CLDC11/dist/CLDC11.jar"
if [ -d "Ports/JavaSE/dist/lib" ]; then
  CN1_CLASSPATH+="$(printf ':%s' "$REPO_ROOT"/Ports/JavaSE/dist/lib/*)"
fi

BUILD_DIR="$(mktemp -d "${TMPDIR:-/tmp}/cn1-javase-sim-tests-XXXXXX")"
CLASS_DIR="$BUILD_DIR/classes"
mkdir -p "$CLASS_DIR"

js_log "Compiling JavaSE simulator integration harness"
"$JAVAC_BIN" -cp "$CN1_CLASSPATH" -d "$CLASS_DIR" \
  "$SCRIPT_DIR/javase/lib/SimulatorModeTestApp.java" \
  "$SCRIPT_DIR/javase/lib/SimulatorWindowModeVerifier.java"

MODES=(single multi)
ACTUAL_ENTRIES=()
for mode in "${MODES[@]}"; do
  if [ "$mode" = "single" ]; then
    scenarios=(default landscape component-inspector network-monitor test-recorder)
  else
    scenarios=(default landscape)
  fi
  for scenario in "${scenarios[@]}"; do
    test_name="javase-${mode}-window"
    if [ "$scenario" != "default" ]; then
      test_name="javase-${mode}-${scenario}"
    fi
    png="$RAW_DIR/${test_name}.png"
    js_log "Running simulator verification for mode=$mode scenario=$scenario"
    FULL_SIM_CLASSPATH="$CN1_CLASSPATH:$CLASS_DIR"
    xvfb-run -a -s "-screen 0 2200x1400x24" "$JAVA_BIN" -Djava.awt.headless=false \
      -cp "$FULL_SIM_CLASSPATH" \
      com.codenameone.examples.javase.tests.SimulatorWindowModeVerifier \
      --mode "$mode" \
      --scenario "$scenario" \
      --sim-classpath "$FULL_SIM_CLASSPATH" \
      --skin "$SIM_SKIN_PATH" \
      --screenshot "$png"

    if [ ! -s "$png" ]; then
      js_log "Expected screenshot was not produced for mode=$mode scenario=$scenario" >&2
      exit 11
    fi
    ACTUAL_ENTRIES+=("${test_name}=$png")
  done
done

COMPARE_JSON="$ARTIFACTS_DIR/screenshot-compare.json"
SUMMARY_FILE="$ARTIFACTS_DIR/screenshot-summary.txt"
COMMENT_FILE="$ARTIFACTS_DIR/screenshot-comment.md"

CN1SS_SUCCESS_MESSAGE="✅ JavaSE simulator integration screenshots matched stored baselines."
CN1SS_COMMENT_MARKER="<!-- cn1ss:javase-simulator -->"
CN1SS_COMMENT_LOG_PREFIX="CN1SS-JAVASE"
CN1SS_PREVIEW_SUBDIR="javase-simulator"

cn1ss_process_and_report \
  "JavaSE simulator screenshot updates" \
  "$COMPARE_JSON" \
  "$SUMMARY_FILE" \
  "$COMMENT_FILE" \
  "$SCREENSHOT_REF_DIR" \
  "$PREVIEW_DIR" \
  "$ARTIFACTS_DIR" \
  "${ACTUAL_ENTRIES[@]}"

js_log "JavaSE simulator integration artifacts stored in $ARTIFACTS_DIR"
