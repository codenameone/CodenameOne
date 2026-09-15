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
JAVA_BIN="${CN1SS_DESKTOP_JAVA:-java}"
SIM_JAR="$(ls maven/javase/target/codenameone-javase-*-jar-with-dependencies.jar 2>/dev/null | head -n1 || true)"
CLASSES_DIR="$APP_DIR/common/target/classes"
if [ ! -f "$CLASSES_DIR/com/codenameone/fidelity/DesktopTileRunner.class" ]; then
  rf_log "FAILED: the fidelity app is not built ($CLASSES_DIR)."
  rf_log "Build it with: (cd $APP_DIR && ./mvnw -q -pl common install)"
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
"$JAVA_BIN" -Djava.awt.headless=true \
    -Dcn1ss.fidelity.platform="$PLATFORM" \
    -Dcn1ss.fidelity.themeResource="/$THEME_RES.res" \
    -cp "$REPO_ROOT/$SIM_JAR:$REPO_ROOT/$APP_DIR/common/target/classes" \
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

cn1ss_process_fidelity \
  "Desktop fidelity ($PLATFORM, $GOLDEN_SET)" \
  "$WORK_DIR/compare.json" \
  "$WORK_DIR/summary.md" \
  "$WORK_DIR/comment.md" \
  "$GOLDENS_DIR" \
  "$PREVIEW_DIR" \
  "$ARTIFACTS_DIR" \
  "$BASELINE_FILE" \
  "$PLATFORM=$TILE_DIR"
