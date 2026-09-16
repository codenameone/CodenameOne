#!/usr/bin/env bash
###
# Score the Codename One desktop native themes against the captured native references.
#
# Usage: run-desktop-fidelity-tests.sh <windows|macos|gnome>
#
# Runs the Codename One side through the JAVASE port rather than a native desktop port. That
# is a deliberate choice for the PR-gating leg: the JavaSE simulator renders the same theme
# through the same core, starts in seconds rather than after a ParparVM translation and a
# native toolchain build, and is where most desktop Codename One applications actually run.
# The native Windows, Linux and macOS ports get a separate, slower leg.
#
# Each theme is scored on ITS OWN platform's runner. A Fluent theme measured on a Mac would
# be measured in the wrong system font, and text metrics are most of a fidelity score -- so
# there is no point running all three anywhere.
#
# Honours CN1SS_FIDELITY_GOLDEN_SET, FIDELITY_UPDATE_BASELINE and CN1SS_FIDELITY_EPSILON.
###
set -euo pipefail

rf_log() { echo "[run-desktop-fidelity-tests] $1"; }

if [ $# -lt 1 ]; then
  rf_log "Usage: $0 <windows|macos|gnome>" >&2
  exit 2
fi
PLATFORM="$1"

case "$PLATFORM" in
  windows) THEME_RES="WindowsFluentTheme"; DEFAULT_SET="windows-11-fluent" ;;
  macos)   THEME_RES="MacOSAquaTheme";     DEFAULT_SET="macos-aqua" ;;
  gnome)   THEME_RES="GnomeAdwaitaTheme";  DEFAULT_SET="gnome-adwaita" ;;
  *) rf_log "Unknown platform '$PLATFORM' (expected windows, macos or gnome)" >&2; exit 2 ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

APP_DIR="${CN1_APP_DIR:-scripts/fidelity-app}"
GOLDEN_SET="${CN1SS_FIDELITY_GOLDEN_SET:-$DEFAULT_SET}"
GOLDENS_DIR="$APP_DIR/goldens/$GOLDEN_SET"
BASELINE_FILE="$APP_DIR/baseline/${GOLDEN_SET}-fidelity-baseline.json"
SPEC_FILE="$APP_DIR/common/src/main/resources/fidelity-tests.yaml"
mkdir -p "$GOLDENS_DIR" "$(dirname "$BASELINE_FILE")"

CN1SS_HELPER_SOURCE_DIR="$SCRIPT_DIR/common/java"
source "$SCRIPT_DIR/lib/cn1ss.sh"
cn1ss_log() { rf_log "$1"; }

# Compile the shared Java helpers (ProcessScreenshots, FidelityGate, the report
# renderers) and point the library at the JVM that runs them. Without this every
# cn1ss_* helper refuses with "CN1SS_JAVA_BIN is not configured" -- which reads as
# an unset variable rather than as a missing setup call, so it is worth naming.
#
# The SAME JVM renders the tiles and runs the helpers: the helpers need 17+ for
# switch expressions and the simulator needs 11+, while tools/env.sh puts JDK 8 on
# PATH for the framework build. CN1SS_DESKTOP_JAVA is how CI passes the newer one.
JAVA_BIN="${CN1SS_DESKTOP_JAVA:-$(command -v java || true)}"
if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
  rf_log "FAILED: no java on PATH; set CN1SS_DESKTOP_JAVA to a JDK 17+ java binary."
  exit 25
fi
if ! cn1ss_setup "$JAVA_BIN" "$CN1SS_HELPER_SOURCE_DIR"; then
  rf_log "FAILED: could not prepare the Java helpers with $JAVA_BIN"
  exit 25
fi

ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/artifacts/${PLATFORM}-fidelity}"
mkdir -p "$ARTIFACTS_DIR"
TMPDIR="${TMPDIR:-/tmp}"; TMPDIR="${TMPDIR%/}"
WORK_DIR="$(mktemp -d "${TMPDIR}/cn1ss-fid-${PLATFORM}-XXXXXX")"
TILE_DIR="$WORK_DIR/tiles"; mkdir -p "$TILE_DIR"
PREVIEW_DIR="$WORK_DIR/previews"; mkdir -p "$PREVIEW_DIR"

# The golden set is the contract. Scoring against an empty directory would compare every
# tile with nothing, and the comparator reports that as "missing_expected" rather than as a
# score -- which reads as a broken run instead of as an unseeded one, so it is said plainly
# here instead.
if [ -z "$(ls -A "$GOLDENS_DIR" 2>/dev/null | grep -v README || true)" ]; then
  rf_log "No native references in $GOLDENS_DIR."
  rf_log "Capture them first with the manual workflow:"
  rf_log "  gh workflow run fidelity-desktop-native-ref.yml -f targets=$PLATFORM -f mode=capture"
  rf_log "then review and commit them per $APP_DIR/goldens/README.md."
  exit 24
fi

rf_log "Rendering Codename One tiles for $PLATFORM using $THEME_RES"

# The runner is told which platform it is rather than inferring it. JavaSE answers "win",
# "mac" or "linux" from the HOST, which is right for an application and wrong here: the same
# host must be able to render whichever theme it is asked for, and the golden set is named
# for the design generation rather than for the machine.
SIM_JAR="$(ls maven/javase/target/codenameone-javase-*-jar-with-dependencies.jar 2>/dev/null | head -n1 || true)"
CLASSES_DIR="$APP_DIR/common/target/classes"
# The tile renderer is in its own module: it is host code (java.awt, java.io) and
# `common` is compiled as CN1 application code under a bytecode-compliance gate.
RUNNER_CLASSES="$APP_DIR/desktop-runner/target/classes"
if [ ! -f "$RUNNER_CLASSES/com/codenameone/fidelity/DesktopTileRunner.class" ]; then
  rf_log "FAILED: the fidelity app is not built ($RUNNER_CLASSES)."
  rf_log "Build it with: (cd $APP_DIR && ./mvnw -q -pl common,desktop-runner install)"
  exit 27
fi
if [ ! -f "$CLASSES_DIR/fidelity-tests.yaml" ]; then
  # The runner reads the spec off its own classpath, so a classes directory without it
  # renders nothing and would otherwise fail later with a less obvious message.
  rf_log "FAILED: fidelity-tests.yaml is missing from $CLASSES_DIR"
  exit 27
fi
if [ -z "$SIM_JAR" ]; then
  rf_log "FAILED: the JavaSE simulator jar is not built."
  rf_log "Build it with: (cd maven && mvn -pl javase -Plocal-dev-javase -DskipTests install)"
  exit 25
fi

set +e
# Themes/ comes FIRST on the class path, and that order is load-bearing rather than tidy.
# There are THREE copies of every native theme in a built tree -- Themes/ (the build output
# and the single source of truth), a copy the fidelity module's pom stages into its
# target/classes, and a copy BUNDLED INSIDE the javase jar -- and whichever the class loader
# reaches first is the one that gets scored.
#
# Both stale copies were found the same way and neither announced itself: a theme edit
# scored identically to no edit at all, which reads as "that CSS change did nothing" rather
# than as "the change was never loaded". The jar's copy was missing three constants the
# source had; the staged copy goes stale the moment build-native-themes.sh runs without a
# module rebuild behind it.
#
# Putting the build output first means the thing just compiled is the thing measured.
#
# NOT -Djava.awt.headless=true. The JavaSE port creates a real AWT window during
# Display.init and throws HeadlessException when it cannot, so the simulator needs a
# display rather than the absence of one -- which is why every other simulator runner
# here (run-javase-device-tests.sh, archetype-smoke.yml) reaches for xvfb-run on Linux
# instead. macOS and Windows runners have a session already.
#
# useAppFrame=false keeps the simulator's inspector/AppFrame chrome out of the run: it
# is stored as a per-user preference, so without pinning it the tiles depend on what
# the last person to open this app in the simulator happened to click.
DISPLAY_WRAPPER=()
if [ "$(uname -s)" = "Linux" ]; then
  if ! command -v xvfb-run >/dev/null 2>&1; then
    rf_log "FAILED: xvfb-run is required on Linux (apt-get install xvfb)."
    exit 25
  fi
  DISPLAY_WRAPPER=(xvfb-run -a)
fi
${DISPLAY_WRAPPER[@]+"${DISPLAY_WRAPPER[@]}"} "$JAVA_BIN" -Dcn1.simulator.useAppFrame=false \
    -Dcn1ss.fidelity.platform="$PLATFORM" \
    -Dcn1ss.fidelity.themeResource="/$THEME_RES.res" \
    -cp "$REPO_ROOT/Themes:$REPO_ROOT/$CLASSES_DIR:$REPO_ROOT/$RUNNER_CLASSES:$REPO_ROOT/$SIM_JAR" \
    com.codenameone.fidelity.DesktopTileRunner "$PLATFORM" "$THEME_RES" "$TILE_DIR"
rc=$?
set -e
if [ "$rc" -ne 0 ]; then
  rf_log "FAILED: tile rendering exited $rc"
  exit "$rc"
fi

TILES="$(ls -1 "$TILE_DIR"/*.png 2>/dev/null | wc -l | tr -d ' ')"
rf_log "Rendered $TILES tile(s)"
if [ "$TILES" = "0" ]; then
  rf_log "FAILED: no tiles were produced, so there is nothing to score."
  exit 26
fi

export CN1SS_FIDELITY_SPEC="$SPEC_FILE"
export CN1SS_FIDELITY_PLATFORM="$PLATFORM"

# Without this the ratchet is not a gate. cn1ss_process_fidelity runs FidelityGate either
# way, but only TURNS a gate failure into a non-zero return when CN1SS_FAIL_ON_MISMATCH=1
# -- otherwise it logs "reported regressions ... not failing" and returns success.
#
# Measured before it was set: raising one baseline entry by five points produced the
# correct "[gate] FAIL: 1 fidelity regression(s)" on stdout and an exit status of 0. A
# workflow would have gone green on a regression it had just printed.
#
# Defaulted rather than forced, so a local exploratory run can still see every score
# without the run aborting, which is what the other suites do too (run-tv-ui-tests.sh,
# run-watch-ui-tests.sh).
export CN1SS_FAIL_ON_MISMATCH="${CN1SS_FAIL_ON_MISMATCH:-1}"

# One --actual entry per TILE, "<test name>=<png>". The comparator takes files, not
# a directory, and handing it a directory is not a usage error it reports -- it is
# an entry whose path does not exist, so the run dies inside the helper with nothing
# to say. The glob also leaves tile-backgrounds.properties behind, which is read
# from the tile directory rather than passed in.
shopt -s nullglob
declare -a COMPARE_ENTRIES=()
for png in "$TILE_DIR"/*_cn1.png; do
  base="$(basename "$png" .png)"
  COMPARE_ENTRIES+=("${base%_cn1}=${png}")
done
shopt -u nullglob
if [ "${#COMPARE_ENTRIES[@]}" -eq 0 ]; then
  rf_log "FAILED: $TILE_DIR holds no *_cn1.png tiles to score."
  exit 26
fi
rf_log "Scoring ${#COMPARE_ENTRIES[@]} tile(s) against $GOLDEN_SET"

# set -e does not apply to the last command of a script in the way that matters here:
# the status has to be captured and re-raised deliberately, so a gate failure leaves this
# script with a non-zero status rather than whatever the last log line returned.
rc=0
cn1ss_process_fidelity \
  "Desktop fidelity ($PLATFORM, $GOLDEN_SET)" \
  "$WORK_DIR/compare.json" \
  "$WORK_DIR/summary.md" \
  "$WORK_DIR/comment.md" \
  "$GOLDENS_DIR" \
  "$PREVIEW_DIR" \
  "$ARTIFACTS_DIR" \
  "$BASELINE_FILE" \
  "${COMPARE_ENTRIES[@]}" || rc=$?
if [ "$rc" -ne 0 ]; then
  rf_log "FAILED: the fidelity gate reported a regression (rc=$rc)."
  exit "$rc"
fi
