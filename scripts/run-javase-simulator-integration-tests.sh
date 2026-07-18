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
    # Authenticate the release lookup when a token is available. The
    # unauthenticated api.github.com quota is 60 requests/hour per IP, which
    # a PR that fans out many jobs (plus re-runs) can exhaust on a shared
    # runner IP, failing skin resolution with "HTTP Error 403: rate limit
    # exceeded". A token raises the quota to 5000/hour and removes the flake.
    SKIN_GH_TOKEN="${GITHUB_TOKEN:-${GH_TOKEN:-}}"
    SKIN_URL="$(GH_API_TOKEN="$SKIN_GH_TOKEN" python3 - <<'PY'
import json
import os
import time
import urllib.error
import urllib.request

def fetch(token):
    request = urllib.request.Request(
        'https://api.github.com/repos/codenameone/codenameone-skins/releases/latest',
        headers={
            'Accept': 'application/vnd.github+json',
            'User-Agent': 'codenameone-javase-simulator-tests'
        }
    )
    if token:
        request.add_header('Authorization', 'Bearer ' + token)
    with urllib.request.urlopen(request) as response:
        return json.load(response)

# api.github.com returns transient 5xx during incidents (observed: a 503 failing
# this job twice in one evening) and 429 under load; retry those with backoff.
# The curl download below already retries -- the lookup must too. The token only
# exists to raise the rate limit, and during incidents the authenticated pool
# can 503 while anonymous requests succeed, so the final attempts drop it.
token = os.environ.get('GH_API_TOKEN', '')
data = None
last_error = None
for attempt, use_token in enumerate([token, token, token, '', ''], start=1):
    try:
        data = fetch(use_token)
        break
    except (urllib.error.HTTPError, urllib.error.URLError) as err:
        code = getattr(err, 'code', None)
        if code is not None and code < 500 and code != 429:
            raise
        last_error = err
        time.sleep(10 * attempt)
if data is None:
    raise SystemExit('release lookup failed after retries: %s' % last_error)

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
    curl -fL --retry 5 --retry-delay 5 --retry-all-errors -o "$SKIN_ARCHIVE" "$SKIN_URL"
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

# Compile the CSS-driven native themes (iOSModernTheme.res,
# AndroidMaterialTheme.res). The JavaSE ant build copies them out of
# Themes/, with failonerror=false, so a missing pair would silently
# produce a JavaSE.jar without the modern themes - which is precisely
# the failure mode the native-theme verification below catches. Run
# the compiler ourselves so a fresh checkout doesn't surprise CI.
js_log "Building CSS-driven native themes"
"$REPO_ROOT/scripts/build-native-themes.sh"

js_log "Ensuring JavaSE port is built"
"$REPO_ROOT/scripts/ci-install-upstream-jcef-jar.sh"
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
    scenarios=(default landscape component-inspector network-monitor test-recorder ar-demo)
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

# Native-theme override regression coverage. The Simulator's "Native
# Theme" menu writes simulatorNativeTheme and triggers a simulator
# reload; the bundled themes (iOSModernTheme, AndroidMaterialTheme,
# the iOS7 / Android Holo legacy themes) must all resolve through
# JavaSEPort.loadSkinFile's override branch. We exercise each one
# end-to-end: set the preference the menu would set, restart the
# simulator the same way the menu does, and assert the running app
# sees a Resources object whose first theme name matches what we'd
# get from opening the bundled .res directly. This catches both
# "menu wrote the wrong preference" and "preference wasn't honored
# on reload" regressions.
NATIVE_THEME_SCENARIOS=(
  "iOSModernTheme"
  "iOS7Theme"
  "AndroidMaterialTheme"
  "android_holo_light"
)
for theme_key in "${NATIVE_THEME_SCENARIOS[@]}"; do
  case "$theme_key" in
    iOSModernTheme) scenario_label=ios-modern ;;
    iOS7Theme) scenario_label=ios7 ;;
    AndroidMaterialTheme) scenario_label=android-material ;;
    android_holo_light) scenario_label=android-holo ;;
    *) scenario_label="$theme_key" ;;
  esac
  test_name="javase-single-native-theme-${scenario_label}"
  png="$RAW_DIR/${test_name}.png"
  js_log "Running simulator native-theme verification for ${theme_key}"
  FULL_SIM_CLASSPATH="$CN1_CLASSPATH:$CLASS_DIR"
  xvfb-run -a -s "-screen 0 2200x1400x24" "$JAVA_BIN" -Djava.awt.headless=false \
    -cp "$FULL_SIM_CLASSPATH" \
    com.codenameone.examples.javase.tests.SimulatorWindowModeVerifier \
    --mode single \
    --scenario "native-theme-${scenario_label}" \
    --sim-classpath "$FULL_SIM_CLASSPATH" \
    --skin "$SIM_SKIN_PATH" \
    --screenshot "$png" \
    --native-theme "$theme_key"

  if [ ! -s "$png" ]; then
    js_log "Expected screenshot was not produced for native-theme ${theme_key}" >&2
    exit 11
  fi
  ACTUAL_ENTRIES+=("${test_name}=$png")
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
