#!/usr/bin/env bash
# Run instrumentation tests and reconstruct screenshot emitted as chunked Base64 (NO ADB)
set -euo pipefail

ra_log() { echo "[run-android-instrumentation-tests] $1"; }

# ---- Helpers ---------------------------------------------------------------

ensure_dir() { mkdir -p "$1" 2>/dev/null || true; }

download_with_tools() {
  local url="$1"
  local dest="$2"
  local tmp="$dest.tmp"
  rm -f "$tmp" 2>/dev/null || true
  if command -v curl >/dev/null 2>&1; then
    if curl -fsSL "$url" -o "$tmp"; then
      mv "$tmp" "$dest"
      return 0
    fi
  fi
  if command -v wget >/dev/null 2>&1; then
    if wget -q -O "$tmp" "$url"; then
      mv "$tmp" "$dest"
      return 0
    fi
  fi
  rm -f "$tmp" 2>/dev/null || true
  return 1
}

# CN1SS helpers are implemented in Java for easier maintenance
CN1SS_MAIN_CLASS="Cn1ssChunkTools"
POST_COMMENT_CLASS="PostPrComment"
PROCESS_SCREENSHOTS_CLASS="ProcessScreenshots"
RENDER_SCREENSHOT_REPORT_CLASS="RenderScreenshotReport"

# ---- Args & environment ----------------------------------------------------

if [ $# -lt 1 ]; then
  ra_log "Usage: $0 <gradle_project_dir>" >&2
  exit 2
fi
GRADLE_PROJECT_DIR="$1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/android/tests"
if [ ! -f "$CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" ]; then
  ra_log "Missing CN1SS helper: $CN1SS_HELPER_SOURCE_DIR/$CN1SS_MAIN_CLASS.java" >&2
  exit 3
fi

source "$SCRIPT_DIR/lib/cn1ss.sh"
cn1ss_log() { ra_log "$1"; }

TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
ensure_dir "$ARTIFACTS_DIR"
TEST_LOG="$ARTIFACTS_DIR/connectedAndroidTest.log"
SCREENSHOT_REF_DIR="$SCRIPT_DIR/android/screenshots"
SCREENSHOT_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1ss-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1ss-tmp")"
ensure_dir "$SCREENSHOT_TMP_DIR"
SCREENSHOT_PREVIEW_DIR="$SCREENSHOT_TMP_DIR/previews"
CLASSFILE_TMP_DIR=""

ra_log "Loading workspace environment from $ENV_FILE"
[ -f "$ENV_FILE" ] || { ra_log "Missing env file: $ENV_FILE"; exit 3; }
# shellcheck disable=SC1090
source "$ENV_FILE"

if [ -z "${JAVA17_HOME:-}" ]; then
  ra_log "JAVA17_HOME not set in workspace environment" >&2
  exit 3
fi

JAVA17_BIN="$JAVA17_HOME/bin/java"
if [ ! -x "$JAVA17_BIN" ]; then
  ra_log "JDK 17 java binary missing at $JAVA17_BIN" >&2
  exit 3
fi

cn1ss_setup "$JAVA17_BIN" "$CN1SS_HELPER_SOURCE_DIR"

[ -d "$GRADLE_PROJECT_DIR" ] || { ra_log "Gradle project directory not found: $GRADLE_PROJECT_DIR"; exit 4; }
[ -x "$GRADLE_PROJECT_DIR/gradlew" ] || chmod +x "$GRADLE_PROJECT_DIR/gradlew"

MANIFEST="$GRADLE_PROJECT_DIR/app/src/main/AndroidManifest.xml"
if [ ! -f "$MANIFEST" ]; then
  ra_log "FATAL: AndroidManifest.xml not found at $MANIFEST" >&2
  exit 10
fi
PACKAGE_NAME="$(sed -n 's/.*package="\([^"]*\)".*/\1/p' "$MANIFEST" | head -n1)"
if [ -z "$PACKAGE_NAME" ]; then
  ra_log "FATAL: Unable to determine package name from AndroidManifest.xml" >&2
  exit 10
fi
ra_log "Detected application package: $PACKAGE_NAME"

if ! command -v adb >/dev/null 2>&1; then
  ra_log "FATAL: adb not found on PATH" >&2
  exit 10
fi

ADB_BIN="$(command -v adb)"
"$ADB_BIN" start-server >/dev/null 2>&1 || true
"$ADB_BIN" wait-for-device
ra_log "ADB connected devices:"
"$ADB_BIN" devices -l | sed 's/^/[run-android-instrumentation-tests]   /'

ra_log "Clearing logcat buffer"
"$ADB_BIN" logcat -c || true

LOGCAT_PID=0
cleanup() {
  if [ "$LOGCAT_PID" -ne 0 ]; then
    kill "$LOGCAT_PID" >/dev/null 2>&1 || true
    wait "$LOGCAT_PID" 2>/dev/null || true
  fi
  "$ADB_BIN" shell am force-stop "$PACKAGE_NAME" >/dev/null 2>&1 || true
  if [ -n "${CLASSFILE_TMP_DIR:-}" ] && [ -d "$CLASSFILE_TMP_DIR" ]; then
    rm -rf "$CLASSFILE_TMP_DIR" 2>/dev/null || true
  fi
}
trap cleanup EXIT

ra_log "Capturing device logcat to $TEST_LOG"
"$ADB_BIN" logcat -v threadtime > "$TEST_LOG" 2>&1 &
LOGCAT_PID=$!

sleep 2

GRADLEW="$GRADLE_PROJECT_DIR/gradlew"
if [ ! -x "$GRADLEW" ]; then
  chmod +x "$GRADLEW"
fi

ra_log "Running connected Android instrumentation tests"
ORIGINAL_JAVA_HOME="${JAVA_HOME:-}"
export JAVA_HOME="$JAVA17_HOME"
GRADLE_RC=0
if ! (
  cd "$GRADLE_PROJECT_DIR" &&
  {
    if command -v sdkmanager >/dev/null 2>&1; then
      yes | sdkmanager --licenses >/dev/null 2>&1 || true
    elif [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
      yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
    fi
    ./gradlew --no-daemon connectedDebugAndroidTest
  }
); then
  GRADLE_RC=$?
fi
export JAVA_HOME="$ORIGINAL_JAVA_HOME"

sleep 2

declare -a CN1SS_SOURCES=("LOGCAT:$TEST_LOG")


# ---- Chunk accounting (diagnostics) ---------------------------------------

LOGCAT_CHUNKS="$(cn1ss_count_chunks "$TEST_LOG")"
LOGCAT_CHUNKS="${LOGCAT_CHUNKS//[^0-9]/}"; : "${LOGCAT_CHUNKS:=0}"

ra_log "Chunk counts -> logcat: ${LOGCAT_CHUNKS}"

if [ "${LOGCAT_CHUNKS:-0}" = "0" ]; then
  ra_log "STAGE:MARKERS_NOT_FOUND -> DeviceRunner output did not include CN1SS chunks"
  ra_log "---- CN1SS lines from logcat ----"
  (grep "CN1SS:" "$TEST_LOG" || true) | sed 's/^/[CN1SS] /'
  exit 12
fi

# ---- Identify CN1SS test streams -----------------------------------------

TEST_NAMES_RAW="$(cn1ss_list_tests "$TEST_LOG" 2>/dev/null | awk 'NF' | sort -u || true)"
declare -a TEST_NAMES=()
if [ -n "$TEST_NAMES_RAW" ]; then
  while IFS= read -r name; do
    [ -n "$name" ] || continue
    TEST_NAMES+=("$name")
  done <<< "$TEST_NAMES_RAW"
else
  TEST_NAMES+=("default")
fi
ra_log "Detected CN1SS test streams: ${TEST_NAMES[*]}"

declare -A TEST_OUTPUTS=()
declare -A TEST_SOURCES=()
declare -A PREVIEW_OUTPUTS=()

ensure_dir "$SCREENSHOT_PREVIEW_DIR"

for test in "${TEST_NAMES[@]}"; do
  dest="$SCREENSHOT_TMP_DIR/${test}.png"
  if source_label="$(cn1ss_decode_test_png "$test" "$dest" "${CN1SS_SOURCES[@]}")"; then
    TEST_OUTPUTS["$test"]="$dest"
    TEST_SOURCES["$test"]="$source_label"
    ra_log "Decoded screenshot for '$test' (source=${source_label}, size: $(cn1ss_file_size "$dest") bytes)"
    preview_dest="$SCREENSHOT_PREVIEW_DIR/${test}.jpg"
    if preview_source="$(cn1ss_decode_test_preview "$test" "$preview_dest" "${CN1SS_SOURCES[@]}")"; then
      PREVIEW_OUTPUTS["$test"]="$preview_dest"
      ra_log "Decoded preview for '$test' (source=${preview_source}, size: $(cn1ss_file_size "$preview_dest") bytes)"
    else
      rm -f "$preview_dest" 2>/dev/null || true
    fi
  else
    ra_log "WARN: Failed to extract/decode CN1SS payload for test '$test'; continuing without screenshot"
    rm -f "$dest" 2>/dev/null || true
    RAW_B64_OUT="$SCREENSHOT_TMP_DIR/${test}.raw.b64"
    if cn1ss_extract_base64 "$TEST_LOG" "$test" > "$RAW_B64_OUT" 2>/dev/null; then
      if [ -s "$RAW_B64_OUT" ]; then
        head -c 64 "$RAW_B64_OUT" | sed 's/^/[CN1SS-B64-HEAD] /'
        ra_log "Partial base64 saved at: $RAW_B64_OUT"
      else
        rm -f "$RAW_B64_OUT" 2>/dev/null || true
      fi
    else
      rm -f "$RAW_B64_OUT" 2>/dev/null || true
    fi
    TEST_OUTPUTS["$test"]="$dest"
    TEST_SOURCES["$test"]="missing"
    ra_log "  -> Marked '$test' as missing actual screenshot (placeholder path: $dest)"
    continue
  fi
done

# ---- Compare against stored references ------------------------------------

COMPARE_ARGS=()
for test in "${TEST_NAMES[@]}"; do
  dest="${TEST_OUTPUTS[$test]:-}"
  [ -n "$dest" ] || continue
  COMPARE_ARGS+=("--actual" "${test}=${dest}")
done

COMPARE_JSON="$SCREENSHOT_TMP_DIR/screenshot-compare.json"
export CN1SS_PREVIEW_DIR="$SCREENSHOT_PREVIEW_DIR"
ra_log "STAGE:COMPARE -> Evaluating screenshots against stored references"
if ! cn1ss_java_run "$PROCESS_SCREENSHOTS_CLASS" \
  --reference-dir "$SCREENSHOT_REF_DIR" \
  --emit-base64 \
  --preview-dir "$SCREENSHOT_PREVIEW_DIR" \
  "${COMPARE_ARGS[@]}" > "$COMPARE_JSON"; then
  ra_log "FATAL: Screenshot comparison helper failed"
  exit 13
fi

SUMMARY_FILE="$SCREENSHOT_TMP_DIR/screenshot-summary.txt"
COMMENT_FILE="$SCREENSHOT_TMP_DIR/screenshot-comment.md"

ra_log "STAGE:COMMENT_BUILD -> Rendering summary and PR comment markdown"
if ! cn1ss_java_run "$RENDER_SCREENSHOT_REPORT_CLASS" \
  --compare-json "$COMPARE_JSON" \
  --comment-out "$COMMENT_FILE" \
  --summary-out "$SUMMARY_FILE"; then
  ra_log "FATAL: Failed to render screenshot summary/comment"
  exit 14
fi

if [ -s "$SUMMARY_FILE" ]; then
  ra_log "  -> Wrote summary entries to $SUMMARY_FILE ($(wc -l < "$SUMMARY_FILE" 2>/dev/null || echo 0) line(s))"
else
  ra_log "  -> No summary entries generated (all screenshots matched stored baselines)"
fi

if [ -s "$COMMENT_FILE" ]; then
  ra_log "  -> Prepared PR comment payload at $COMMENT_FILE (bytes=$(wc -c < "$COMMENT_FILE" 2>/dev/null || echo 0))"
else
  ra_log "  -> No PR comment content produced"
fi

if [ -s "$SUMMARY_FILE" ]; then
  while IFS='|' read -r status test message copy_flag path preview_note; do
    [ -n "${test:-}" ] || continue
    ra_log "Test '${test}': ${message}"
    if [ "$copy_flag" = "1" ] && [ -n "${path:-}" ] && [ -f "$path" ]; then
      cp -f "$path" "$ARTIFACTS_DIR/${test}.png" 2>/dev/null || true
      ra_log "  -> Stored PNG artifact copy at $ARTIFACTS_DIR/${test}.png"
    fi
    if [ "$status" = "equal" ] && [ -n "${path:-}" ]; then
      rm -f "$path" 2>/dev/null || true
    fi
    if [ -n "${preview_note:-}" ]; then
      ra_log "  Preview note: ${preview_note}"
    fi
done < "$SUMMARY_FILE"
fi

ensure_dir "$ARTIFACTS_DIR/android-coverage"
COVERAGE_ARTIFACT_DIR="$ARTIFACTS_DIR/android-coverage"
COVERAGE_JSON="$COVERAGE_ARTIFACT_DIR/coverage.json"
CLASSFILE_TMP_DIR="$(mktemp -d "${TMPDIR}/cn1ss-classfiles-XXXXXX" 2>/dev/null || mktemp -d)"
PORT_PACKAGE_PATH="com/codename1/impl/android"
PORT_PACKAGE_GLOB="*${PORT_PACKAGE_PATH}*"
printf '{"available": false}\n' > "$COVERAGE_JSON"

declare -a CLASSFILE_ARGS=()
declare -a CLASSFILE_SOURCES=()
declare -A CLASSFILE_SEEN=()

is_instrumented_class_path() {
  local path="$1"
  case "$path" in
    *jacocoDebug*|*jacoco-instrumented*|*jacocoInstrumented*|*jacoco/classes*|*jacoco-classes*|*jacoco/test*|*jacoco/androidTest*|*jacoco-coverage*|*jacoco/*)
      return 0
      ;;
  esac
  return 1
}

write_coverage_unavailable() {
  local reason="$1"
  python3 - "$COVERAGE_JSON" "$reason" <<'PY'
import json
import sys
from pathlib import Path

out_path = Path(sys.argv[1])
reason = sys.argv[2]
out_path.write_text(json.dumps({"available": False, "note": reason}) + "\n")
PY
}

add_classfile_source() {
  local path="$1"
  [ -n "$path" ] || return
  if is_instrumented_class_path "$path"; then
    ra_log "Skipping instrumented class path: $path"
    return
  fi
  if [ -n "${CLASSFILE_SEEN[$path]:-}" ]; then
    return
  fi
  if [ -d "$path" ]; then
    if find "$path" -type f -name '*.class' -path "$PORT_PACKAGE_GLOB" -print -quit >/dev/null 2>&1; then
      CLASSFILE_ARGS+=(--classfiles "$path")
      CLASSFILE_SOURCES+=("$path (directory)")
      CLASSFILE_SEEN[$path]=1
    fi
  elif [ -f "$path" ]; then
    if unzip -l "$path" "${PORT_PACKAGE_PATH}/*" >/dev/null 2>&1; then
      local jar_dir
      jar_dir="$CLASSFILE_TMP_DIR/jar-$(( ${#CLASSFILE_SOURCES[@]} + 1 ))"
      rm -rf "$jar_dir" 2>/dev/null || true
      ensure_dir "$jar_dir"
      if unzip -qo "$path" "${PORT_PACKAGE_PATH}/*" -d "$jar_dir" >/dev/null 2>&1; then
        if find "$jar_dir" -type f -name '*.class' -path "$PORT_PACKAGE_GLOB" -print -quit >/dev/null 2>&1; then
          CLASSFILE_ARGS+=(--classfiles "$jar_dir")
          CLASSFILE_SOURCES+=("$path (jar extracted to $jar_dir)")
          CLASSFILE_SEEN[$path]=1
        else
          rm -rf "$jar_dir" 2>/dev/null || true
        fi
      else
        rm -rf "$jar_dir" 2>/dev/null || true
        ra_log "WARN: Failed to extract Codename One classes from jar $path"
      fi
    fi
  fi
}

COVERAGE_EXEC="$(find "$GRADLE_PROJECT_DIR" \( -path '*/outputs/code_coverage/*/connected/*.ec' -o -name 'coverage.ec' \) -type f | head -n 1 || true)"
if [ -n "$COVERAGE_EXEC" ] && [ -f "$COVERAGE_EXEC" ]; then
  ra_log "Detected instrumentation coverage file at $COVERAGE_EXEC"
  PORT_CLASSES_DIR="$REPO_ROOT/maven/android/target/classes"
  if [ ! -d "$PORT_CLASSES_DIR" ]; then
    ALT_CLASSES_DIR="$(find "$REPO_ROOT/maven/android" -maxdepth 4 -type d -name classes | head -n 1 || true)"
    if [ -n "$ALT_CLASSES_DIR" ] && [ -d "$ALT_CLASSES_DIR" ]; then
      PORT_CLASSES_DIR="$ALT_CLASSES_DIR"
    else
      PORT_CLASSES_DIR=""
    fi
  fi
  PORT_JAR="$(find "$REPO_ROOT/maven/android" -maxdepth 2 -type f -name '*.jar' -print 2>/dev/null | head -n 1 || true)"
  add_classfile_source "$PORT_CLASSES_DIR"
  add_classfile_source "$PORT_JAR"

  while IFS= read -r -d '' classfile; do
    if is_instrumented_class_path "$classfile"; then
      continue
    fi
    package_needle="${PORT_PACKAGE_PATH}/"
    class_root="${classfile%%$package_needle*}"
    if [ -n "$class_root" ] && [ "$class_root" != "$classfile" ]; then
      add_classfile_source "${class_root%/}"
    else
      add_classfile_source "$(dirname "$classfile")"
    fi
  done < <(find "$GRADLE_PROJECT_DIR/app/build" -type f -name '*.class' -path "$PORT_PACKAGE_GLOB" -print0 2>/dev/null)

  while IFS= read -r candidate; do
    if is_instrumented_class_path "$candidate"; then
      continue
    fi
    add_classfile_source "$candidate"
  done < <(find "$GRADLE_PROJECT_DIR/app/build" -maxdepth 6 -type f \( -name 'classes.jar' -o -name '*.jar' \) 2>/dev/null)

  if [ ${#CLASSFILE_SOURCES[@]} -gt 0 ]; then
    ra_log "Using classfile inputs for coverage:"
    for src in "${CLASSFILE_SOURCES[@]}"; do
      ra_log "  -> $src"
    done
  fi
  if [ ${#CLASSFILE_ARGS[@]} -eq 0 ]; then
    ra_log "WARN: Port class files not found; skipping coverage report generation"
    write_coverage_unavailable "Port class files not found in Gradle outputs"
  else
    JACOCO_VERSION="${JACOCO_VERSION:-0.8.11}"
    JACOCO_CACHE_DIR="$DOWNLOAD_DIR/jacoco/$JACOCO_VERSION"
    ensure_dir "$JACOCO_CACHE_DIR"
    JACOCO_NODEPS_JAR="$JACOCO_CACHE_DIR/org.jacoco.cli-$JACOCO_VERSION-nodeps.jar"
    JACOCO_CLI_JAR="$JACOCO_CACHE_DIR/org.jacoco.cli-$JACOCO_VERSION.jar"
    ARGS4J_VERSION="${ARGS4J_VERSION:-2.33}"
    JACOCO_ARGS4J_JAR="$JACOCO_CACHE_DIR/args4j-$ARGS4J_VERSION.jar"

    if [ ! -f "$JACOCO_NODEPS_JAR" ]; then
      ra_log "Downloading JaCoCo CLI (nodeps) $JACOCO_VERSION"
      JACOCO_NODEPS_URL="https://repo1.maven.org/maven2/org/jacoco/org.jacoco.cli/$JACOCO_VERSION/org.jacoco.cli-$JACOCO_VERSION-nodeps.jar"
      if ! download_with_tools "$JACOCO_NODEPS_URL" "$JACOCO_NODEPS_JAR"; then
        ra_log "WARN: Failed to download JaCoCo CLI nodeps jar from $JACOCO_NODEPS_URL"
        if [ -n "${MAVEN_HOME:-}" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
          ra_log "Falling back to Maven dependency:copy for JaCoCo CLI nodeps jar"
          "$MAVEN_HOME/bin/mvn" -B -q dependency:copy \
            -Dartifact=org.jacoco:org.jacoco.cli:$JACOCO_VERSION:jar:nodeps \
            -DoutputDirectory="$JACOCO_CACHE_DIR" || ra_log "WARN: Maven download of JaCoCo CLI nodeps jar failed"
        fi
      fi
    fi

    if [ ! -f "$JACOCO_CLI_JAR" ]; then
      ra_log "Downloading JaCoCo CLI $JACOCO_VERSION"
      JACOCO_URL="https://repo1.maven.org/maven2/org/jacoco/org.jacoco.cli/$JACOCO_VERSION/org.jacoco.cli-$JACOCO_VERSION.jar"
      if ! download_with_tools "$JACOCO_URL" "$JACOCO_CLI_JAR"; then
        ra_log "WARN: Failed to download JaCoCo CLI jar from $JACOCO_URL"
        if [ -n "${MAVEN_HOME:-}" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
          ra_log "Falling back to Maven dependency:copy for JaCoCo CLI"
          "$MAVEN_HOME/bin/mvn" -B -q dependency:copy \
            -Dartifact=org.jacoco:org.jacoco.cli:$JACOCO_VERSION:jar \
            -DoutputDirectory="$JACOCO_CACHE_DIR" || ra_log "WARN: Maven download of JaCoCo CLI failed"
        fi
      fi
    fi

    JACOCO_CMD=()
    JACOCO_MAIN_CLASS="org.jacoco.cli.internal.Main"
    if [ -f "$JACOCO_NODEPS_JAR" ]; then
      JACOCO_CMD=("$JAVA17_BIN" "-jar" "$JACOCO_NODEPS_JAR")
    elif [ -f "$JACOCO_CLI_JAR" ]; then
      if unzip -p "$JACOCO_CLI_JAR" META-INF/MANIFEST.MF 2>/dev/null | grep -qi 'Main-Class:'; then
        JACOCO_CMD=("$JAVA17_BIN" "-jar" "$JACOCO_CLI_JAR")
      else
        if [ ! -f "$JACOCO_ARGS4J_JAR" ]; then
          ra_log "Downloading args4j dependency $ARGS4J_VERSION for JaCoCo CLI"
          ARGS4J_URL="https://repo1.maven.org/maven2/org/kohsuke/args4j/$ARGS4J_VERSION/args4j-$ARGS4J_VERSION.jar"
          if ! download_with_tools "$ARGS4J_URL" "$JACOCO_ARGS4J_JAR"; then
            ra_log "WARN: Failed to download args4j from $ARGS4J_URL"
            if [ -n "${MAVEN_HOME:-}" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
              ra_log "Falling back to Maven dependency:copy for args4j"
              "$MAVEN_HOME/bin/mvn" -B -q dependency:copy \
                -Dartifact=org.kohsuke:args4j:$ARGS4J_VERSION:jar \
                -DoutputDirectory="$JACOCO_CACHE_DIR" || ra_log "WARN: Maven download of args4j failed"
            fi
          fi
        fi
        if [ -f "$JACOCO_ARGS4J_JAR" ]; then
          case "$(uname -s)" in
            MINGW*|MSYS*|CYGWIN*) JACOCO_PATHSEP=';';;
            *) JACOCO_PATHSEP=':';;
          esac
          JACOCO_CMD=("$JAVA17_BIN" "-cp" "$JACOCO_CLI_JAR${JACOCO_PATHSEP}$JACOCO_ARGS4J_JAR" "$JACOCO_MAIN_CLASS")
        else
          ra_log "WARN: args4j dependency missing; cannot execute JaCoCo CLI"
        fi
      fi
    fi

    if [ ${#JACOCO_CMD[@]} -gt 0 ]; then
      COVERAGE_SITE_DIR="$COVERAGE_ARTIFACT_DIR/site/jacoco"
      ensure_dir "$COVERAGE_SITE_DIR"
      COVERAGE_XML="$COVERAGE_SITE_DIR/jacoco.xml"
      JACOCO_LOG="$COVERAGE_ARTIFACT_DIR/jacoco-report.log"
      ra_log "Generating JaCoCo report for Android port sources"
      if "${JACOCO_CMD[@]}" report "$COVERAGE_EXEC" \
        --name "CodenameOneAndroidPort" \
        "${CLASSFILE_ARGS[@]}" \
        --sourcefiles "$REPO_ROOT/Ports/Android/src" \
        --html "$COVERAGE_SITE_DIR" \
        --xml "$COVERAGE_XML" \
        >"$JACOCO_LOG" 2>&1; then
        ra_log "JaCoCo report generated at $COVERAGE_SITE_DIR"
        python3 - <<'PY' "$COVERAGE_XML" "$COVERAGE_JSON"
import json
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

xml_path = Path(sys.argv[1])
out_path = Path(sys.argv[2])
if not xml_path.is_file():
    out_path.write_text(json.dumps({"available": False}))
    sys.exit(0)

root = ET.parse(xml_path).getroot()
covered = missed = 0
for counter in root.findall('counter'):
    if counter.attrib.get('type') == 'LINE':
        covered += int(counter.attrib.get('covered', '0'))
        missed += int(counter.attrib.get('missed', '0'))
total = covered + missed
percent = 0.0
if total > 0:
    percent = covered / total * 100.0
data = {
    "available": True,
    "lines": {
        "covered": covered,
        "missed": missed,
        "total": total,
        "percent": percent
    }
}
out_path.write_text(json.dumps(data) + "\n")
PY
      else
        ra_log "WARN: JaCoCo report generation failed (see $JACOCO_LOG)"
        if [ -f "$JACOCO_LOG" ]; then
          tail -n 20 "$JACOCO_LOG" | sed 's/^/[run-android-instrumentation-tests]   /'
        fi
        write_coverage_unavailable "JaCoCo report generation failed (see jacoco-report.log)"
      fi
    else
      ra_log "WARN: JaCoCo CLI executable unavailable; coverage report not generated"
      write_coverage_unavailable "JaCoCo CLI unavailable"
    fi
    cp -f "$COVERAGE_EXEC" "$COVERAGE_ARTIFACT_DIR/coverage.ec" 2>/dev/null || true
  fi
else
  ra_log "WARN: Coverage execution data not found; skipping report generation"
  write_coverage_unavailable "Coverage execution (.ec) file missing"
fi

rm -rf "$CLASSFILE_TMP_DIR" 2>/dev/null || true
CLASSFILE_TMP_DIR=""

cp -f "$COMPARE_JSON" "$ARTIFACTS_DIR/screenshot-compare.json" 2>/dev/null || true
if [ -s "$COMMENT_FILE" ]; then
  cp -f "$COMMENT_FILE" "$ARTIFACTS_DIR/screenshot-comment.md" 2>/dev/null || true
fi

PREVIEW_ARTIFACT_DIR="$ARTIFACTS_DIR/android-previews"
if [ -d "$SCREENSHOT_PREVIEW_DIR" ]; then
  if find "$SCREENSHOT_PREVIEW_DIR" -maxdepth 1 -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' \) -print -quit >/dev/null; then
    rm -rf "$PREVIEW_ARTIFACT_DIR" 2>/dev/null || true
    mkdir -p "$PREVIEW_ARTIFACT_DIR"
    cp -R "$SCREENSHOT_PREVIEW_DIR"/. "$PREVIEW_ARTIFACT_DIR/"
  fi
fi

# Copy useful artifacts for GH Actions
cp -f "$TEST_LOG" "$ARTIFACTS_DIR/device-runner-logcat.txt" 2>/dev/null || true
[ -n "${TEST_EXEC_LOG:-}" ] && cp -f "$TEST_EXEC_LOG" "$ARTIFACTS_DIR/test-results.log" 2>/dev/null || true

if [ -z "${CN1SS_SKIP_COMMENT:-}" ]; then
  ra_log "STAGE:COMMENT_POST -> Submitting PR feedback"
  comment_rc=0
  export CN1SS_COMMENT_MARKER="<!-- CN1SS_ANDROID_COMMENT -->"
  export CN1SS_COMMENT_LOG_PREFIX="[run-android-device-tests]"
  if ! cn1ss_post_pr_comment "$COMMENT_FILE" "$SCREENSHOT_PREVIEW_DIR"; then
    comment_rc=$?
  fi
  exit $comment_rc
fi

exit $GRADLE_RC
