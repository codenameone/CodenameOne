#!/usr/bin/env bash
# Windows simulator + tooling screenshot smoke tests.
#
# Runs under Git Bash on a windows-latest GitHub runner (which has a real
# desktop session, so no xvfb equivalent is needed). Prerequisites, provided
# by .github/workflows/windows-tooling.yml:
#   - the maven/ reactor installed to the local repo (JDK 8 build)
#   - the scripts/settings tool installed to the local repo (JDK 17 build)
#   - JAVA_HOME pointing at JDK 17 for the runtime below
#
# Coverage:
#   1. cn1:settings end-to-end through the REAL mojo launch path (javaw.exe
#      launcher, binding file, file:// URL round trip) against a project in a
#      directory WITH SPACES, asserting the captured window isn't the black
#      screen from issue #5443.
#   2. The JavaSE simulator (single + multi window) via the shared
#      SimulatorWindowModeVerifier harness, which self-validates content.
set -euo pipefail

wt_log() { echo "[run-windows-tooling-tests] $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if ! command -v cygpath >/dev/null 2>&1; then
  wt_log "cygpath not found - this script must run under Git Bash on Windows" >&2
  exit 2
fi

winpath() { cygpath -w "$1"; }

# Keep every path the shell tools touch in POSIX form: GNU tar (and friends)
# treat the colon in D:\a\_temp as a remote-host separator. Windows form is
# produced via winpath only at java/maven argument boundaries.
TEMP_BASE="$(cygpath -u "${RUNNER_TEMP:-${TMPDIR:-/tmp}}")"

JAVA_BIN="${JAVA_HOME:?JAVA_HOME (JDK 17+) must be set}/bin/java"
JAVAC_BIN="$JAVA_HOME/bin/javac"

CN1_VERSION=$(awk -F'[<>]' '/<version>/{print $3; exit}' maven/pom.xml)
wt_log "Codename One version: $CN1_VERSION"

ARTIFACTS_BASE="$(cygpath -u "${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}")"
ARTIFACTS_DIR="$ARTIFACTS_BASE/windows-tooling-tests"
mkdir -p "$ARTIFACTS_DIR"
ARTIFACTS_W="$(winpath "$ARTIFACTS_DIR")"

SANITY_SRC_W="$(winpath "$SCRIPT_DIR/windows/ScreenshotSanity.java")"

# ---------------------------------------------------------------------------
# 1. Settings tool (Control Center) through the real cn1:settings mojo.
#    The directory deliberately contains spaces on both levels: Windows user
#    dirs commonly do, and the file://-URL round trip must survive them.
# ---------------------------------------------------------------------------
WORK_DIR="$TEMP_BASE/cn1 tooling tests/Demo App"
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"

cat > "$WORK_DIR/codenameone_settings.properties" <<'EOF'
codename1.displayName=WindowsToolingDemo
codename1.packageName=com.codename1.demos.windows
codename1.mainName=WindowsToolingDemo
codename1.version=1.0
codename1.vendor=Codename One
codename1.icon=icon.png
EOF

cat > "$WORK_DIR/pom.xml" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.codename1.demos</groupId>
  <artifactId>windows-tooling-demo</artifactId>
  <version>1.0</version>
  <packaging>jar</packaging>
</project>
EOF

SETTINGS_PNG="$ARTIFACTS_DIR/settings.png"
wt_log "Launching cn1:settings against '$WORK_DIR' (screenshot mode)"
mvn -B -f "$(winpath "$WORK_DIR/pom.xml")" \
  "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:settings" \
  -Dsettings.spawn=false \
  "-Dsettings.screenshot=$(winpath "$SETTINGS_PNG")" \
  -Dsettings.screenshot.delay=8000

wt_log "Validating settings screenshot"
"$JAVA_BIN" "$SANITY_SRC_W" "$(winpath "$SETTINGS_PNG")" 800 400

# The settings log is the primary diagnostic for launch failures - keep it
# with the artifacts either way.
if [ -f "$HOME/.codenameoneSettings/settings.log" ]; then
  cp "$HOME/.codenameoneSettings/settings.log" "$ARTIFACTS_DIR/settings.log" || true
fi

# ---------------------------------------------------------------------------
# 1b. Settings render matrix.
#     The mojo run above covers the launch path on the runner's own desktop:
#     100% scale, light mode, en-US. Real Windows 11 desktops are routinely
#     none of those, and issue #5443 survived a fix that this single
#     configuration was green for. The matrix drives the launcher directly
#     (same binding file the mojo writes) so each run can vary the JVM's HiDPI
#     scale, dark mode and locale, and captures the on-screen pixels plus a
#     render-state dump next to every screenshot.
# ---------------------------------------------------------------------------
SETTINGS_CP_DIR="$TEMP_BASE/cn1-windows-settings-cp"
mkdir -p "$SETTINGS_CP_DIR"
cat > "$SETTINGS_CP_DIR/cp-pom.xml" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.codename1.demos</groupId>
  <artifactId>windows-settings-classpath</artifactId>
  <version>1.0</version>
  <packaging>pom</packaging>
  <dependencies>
    <dependency>
      <groupId>com.codenameone</groupId>
      <artifactId>codenameone-settings</artifactId>
      <version>$CN1_VERSION</version>
    </dependency>
    <dependency>
      <groupId>com.codenameone</groupId>
      <artifactId>codenameone-core</artifactId>
      <version>$CN1_VERSION</version>
    </dependency>
    <dependency>
      <groupId>com.codenameone</groupId>
      <artifactId>codenameone-javase</artifactId>
      <version>$CN1_VERSION</version>
    </dependency>
  </dependencies>
</project>
EOF
mvn -B -q -f "$(winpath "$SETTINGS_CP_DIR/cp-pom.xml")" dependency:build-classpath \
  "-Dmdep.outputFile=$(winpath "$SETTINGS_CP_DIR/cp.txt")"
SETTINGS_CP="$(cat "$SETTINGS_CP_DIR/cp.txt")"

BINDING_FILE="$SETTINGS_CP_DIR/settings.input"
WORK_DIR_W="$(winpath "$WORK_DIR")"
cat > "$BINDING_FILE" <<EOF
# Codename One Settings project binding
projectDir=$WORK_DIR_W
settings=$WORK_DIR_W\\codenameone_settings.properties
pom=$WORK_DIR_W\\pom.xml
multimoduleRoot=$WORK_DIR_W
EOF

MATRIX_FAILURES=()

# run_settings_scenario <name> [extra jvm args...]
run_settings_scenario() {
  local name="$1"; shift
  local png="$ARTIFACTS_DIR/settings-$name.png"
  local diag="$ARTIFACTS_DIR/settings-$name.txt"
  wt_log "Settings render scenario: $name ($*)"
  if ! "$JAVA_BIN" "$@" \
      -Djava.awt.headless=false \
      "-Dsettings.input=$(winpath "$BINDING_FILE")" \
      "-Dsettings.screenshot=$(winpath "$png")" \
      "-Dsettings.diagnostics=$(winpath "$diag")" \
      -Dsettings.screenshot.delay=8000 \
      -cp "$SETTINGS_CP" \
      com.codename1.settings.CodenameOneSettingsLauncher \
      > "$ARTIFACTS_DIR/settings-$name.log" 2>&1; then
    wt_log "  scenario $name: launcher exited non-zero"
    MATRIX_FAILURES+=("$name (launcher)")
    return 0
  fi
  # The window the user looks at is the one that has to be right, so the
  # on-screen grab is the gate; the offscreen paint is kept for comparison
  # because a disagreement between the two is itself a finding.
  local onscreen="$ARTIFACTS_DIR/settings-$name.onscreen.png"
  local gate="$onscreen"
  if [ ! -s "$gate" ]; then
    wt_log "  scenario $name: no on-screen capture, falling back to offscreen paint"
    gate="$png"
  fi
  # Deliberately loose bounds: the runner desktop is 1024x768, so a scaled
  # window is legitimately small. The colour/flatness checks are the signal.
  if ! "$JAVA_BIN" "$SANITY_SRC_W" "$(winpath "$gate")" 300 200; then
    MATRIX_FAILURES+=("$name")
  fi
}

run_settings_scenario baseline
run_settings_scenario dark -Dsettings.darkMode=true
run_settings_scenario hidpi125 -Dsun.java2d.uiScale=1.25
run_settings_scenario hidpi150 -Dsun.java2d.uiScale=1.5
run_settings_scenario hidpi175 -Dsun.java2d.uiScale=1.75
run_settings_scenario hidpi150-dark -Dsun.java2d.uiScale=1.5 -Dsettings.darkMode=true
run_settings_scenario locale-es -Duser.language=es -Duser.country=ES

if [ ${#MATRIX_FAILURES[@]} -gt 0 ]; then
  wt_log "Settings render matrix failed for: ${MATRIX_FAILURES[*]}" >&2
  exit 1
fi
wt_log "Settings render matrix passed"

# ---------------------------------------------------------------------------
# 2. JavaSE simulator smoke via the shared verifier harness.
# ---------------------------------------------------------------------------
BUILD_DIR="$TEMP_BASE/cn1-windows-sim"
mkdir -p "$BUILD_DIR"

wt_log "Resolving simulator classpath from maven artifacts"
cat > "$BUILD_DIR/cp-pom.xml" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.codename1.demos</groupId>
  <artifactId>windows-sim-classpath</artifactId>
  <version>1.0</version>
  <packaging>pom</packaging>
  <dependencies>
    <dependency>
      <groupId>com.codenameone</groupId>
      <artifactId>codenameone-core</artifactId>
      <version>$CN1_VERSION</version>
    </dependency>
    <dependency>
      <groupId>com.codenameone</groupId>
      <artifactId>codenameone-javase</artifactId>
      <version>$CN1_VERSION</version>
    </dependency>
  </dependencies>
</project>
EOF
mvn -B -q -f "$(winpath "$BUILD_DIR/cp-pom.xml")" dependency:build-classpath \
  "-Dmdep.outputFile=$(winpath "$BUILD_DIR/cp.txt")"
SIM_CP="$(cat "$BUILD_DIR/cp.txt")"

CLASS_DIR="$BUILD_DIR/classes"
mkdir -p "$CLASS_DIR"
wt_log "Compiling simulator verifier harness"
"$JAVAC_BIN" -cp "$SIM_CP" -d "$(winpath "$CLASS_DIR")" \
  "$(winpath "$SCRIPT_DIR/javase/lib/SimulatorModeTestApp.java")" \
  "$(winpath "$SCRIPT_DIR/javase/lib/SimulatorWindowModeVerifier.java")"

SKIN_CACHE_DIR="$TEMP_BASE/cn1-windows-skins"
mkdir -p "$SKIN_CACHE_DIR"
SKIN_ARCHIVE="$SKIN_CACHE_DIR/skins.tar.gz"
SKIN_EXTRACT_DIR="$SKIN_CACHE_DIR/extracted"
if [ ! -s "$SKIN_ARCHIVE" ]; then
  wt_log "Resolving simulator skin from codenameone-skins release"
  AUTH_ARGS=()
  if [ -n "${GITHUB_TOKEN:-${GH_TOKEN:-}}" ]; then
    AUTH_ARGS=(-H "Authorization: Bearer ${GITHUB_TOKEN:-${GH_TOKEN:-}}")
  fi
  SKIN_URL="$(curl -fsSL --retry 5 --retry-delay 5 --retry-all-errors "${AUTH_ARGS[@]}" \
    -H 'Accept: application/vnd.github+json' \
    https://api.github.com/repos/codenameone/codenameone-skins/releases/latest \
    | jq -r '.assets[0].browser_download_url')"
  if [ -z "$SKIN_URL" ] || [ "$SKIN_URL" = "null" ]; then
    wt_log "Failed to resolve codenameone-skins release asset URL" >&2
    exit 2
  fi
  curl -fL --retry 5 --retry-delay 5 --retry-all-errors -o "$SKIN_ARCHIVE" "$SKIN_URL"
fi
if [ ! -d "$SKIN_EXTRACT_DIR" ]; then
  mkdir -p "$SKIN_EXTRACT_DIR"
  tar -xzf "$SKIN_ARCHIVE" -C "$SKIN_EXTRACT_DIR"
fi
SIM_SKIN_PATH="$(find "$SKIN_EXTRACT_DIR" -type f -name 'Nexus5X.skin' | head -n 1 || true)"
if [ -z "$SIM_SKIN_PATH" ]; then
  SIM_SKIN_PATH="$(find "$SKIN_EXTRACT_DIR" -type f -name '*.skin' | head -n 1 || true)"
fi
if [ -z "$SIM_SKIN_PATH" ]; then
  wt_log "Unable to locate a simulator skin file" >&2
  exit 2
fi
wt_log "Using simulator skin: $SIM_SKIN_PATH"

FULL_SIM_CP="$SIM_CP;$(winpath "$CLASS_DIR")"
for mode in single multi; do
  png="$ARTIFACTS_DIR/simulator-$mode-window.png"
  wt_log "Running simulator verification for mode=$mode"
  "$JAVA_BIN" -Djava.awt.headless=false \
    -cp "$FULL_SIM_CP" \
    com.codenameone.examples.javase.tests.SimulatorWindowModeVerifier \
    --mode "$mode" \
    --scenario default \
    --sim-classpath "$FULL_SIM_CP" \
    --skin "$(winpath "$SIM_SKIN_PATH")" \
    --screenshot "$(winpath "$png")"
  wt_log "Validating simulator screenshot for mode=$mode"
  "$JAVA_BIN" "$SANITY_SRC_W" "$(winpath "$png")" 800 600
done

wt_log "All Windows tooling and simulator smoke tests passed"
