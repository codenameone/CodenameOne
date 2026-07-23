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

JAVA_BIN="${JAVA_HOME:?JAVA_HOME (JDK 17+) must be set}/bin/java"
JAVAC_BIN="$JAVA_HOME/bin/javac"

CN1_VERSION=$(awk -F'[<>]' '/<version>/{print $3; exit}' maven/pom.xml)
wt_log "Codename One version: $CN1_VERSION"

ARTIFACTS_BASE="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts}"
ARTIFACTS_DIR="$ARTIFACTS_BASE/windows-tooling-tests"
mkdir -p "$ARTIFACTS_DIR"
ARTIFACTS_W="$(winpath "$ARTIFACTS_DIR")"

SANITY_SRC_W="$(winpath "$SCRIPT_DIR/windows/ScreenshotSanity.java")"

# ---------------------------------------------------------------------------
# 1. Settings tool (Control Center) through the real cn1:settings mojo.
#    The directory deliberately contains spaces on both levels: Windows user
#    dirs commonly do, and the file://-URL round trip must survive them.
# ---------------------------------------------------------------------------
WORK_DIR="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/cn1 tooling tests/Demo App"
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
# 2. JavaSE simulator smoke via the shared verifier harness.
# ---------------------------------------------------------------------------
BUILD_DIR="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/cn1-windows-sim"
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

SKIN_CACHE_DIR="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/cn1-windows-skins"
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
