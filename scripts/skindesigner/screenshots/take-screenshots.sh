#!/usr/bin/env bash
# Generate the per-stage Skin Designer screenshots referenced by the
# developer-guide chapter.
#
# Boots Codename One in quiet mode (no skin window, no source-watcher)
# via mvn exec:java on ScreenshotApp.main. ScreenshotApp walks the
# wizard scenarios on the EDT and writes each Form.toImage() PNG into
# OUT_DIR. Replaces the older Lifecycle + cn1:simulator path that hung
# inside CI.
#
# Run by .github/workflows/website-docs.yml ahead of the Hugo build.
# Locally:
#
#     scripts/skindesigner/screenshots/take-screenshots.sh
#
# Requires Java 17 + Maven on PATH. On Linux you also need xvfb so
# AWT's font/graphics initialisation can succeed (CN1 Display.init
# still pokes Toolkit.getDefaultToolkit()).
set -euo pipefail

log() { echo "[skin-designer-screenshots] $1" >&2; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKIN_DESIGNER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKIN_DESIGNER_DIR/../.." && pwd)"

OUT_DIR="${OUT_DIR:-$REPO_ROOT/docs/developer-guide/img/skin-designer}"
mkdir -p "$OUT_DIR"

SCREENSHOT_NAMES=(
    "skin-designer-stage-1-device"
    "skin-designer-stage-2-source"
    "skin-designer-stage-3-editor-shape"
    "skin-designer-stage-3-editor-cutouts"
    "skin-designer-stage-3-editor-info"
    "skin-designer-stage-4-done"
)

# Wipe stale outputs so a partial run can never report success with old
# files.
for name in "${SCREENSHOT_NAMES[@]}"; do
    rm -f "$OUT_DIR/$name.png"
done

# Ensure the cn1lib's extracted main.zip exists. The Codename One plugin's
# install-cn1lib goal usually creates this on first checkout, but on CI we
# do it ourselves so the build doesn't depend on external state.
ZIP_LIB_DIR="$SKIN_DESIGNER_DIR/cn1libs/ZipSupport"
if [ -f "$SKIN_DESIGNER_DIR/cn1libs/ZipSupport.cn1lib" ] && [ ! -f "$ZIP_LIB_DIR/jars/main.zip" ]; then
    log "Extracting ZipSupport cn1lib"
    mkdir -p "$ZIP_LIB_DIR/jars"
    unzip -p "$SKIN_DESIGNER_DIR/cn1libs/ZipSupport.cn1lib" main.zip > "$ZIP_LIB_DIR/jars/main.zip"
fi

# CN1 plugin steps (CSS compile, compliance check) instantiate JavaSEPort
# which queries getDefaultScreenDevice(); on a CI runner without a display
# that throws HeadlessException. Wrap mvn in xvfb-run when available.
mvn_with_display() {
    if command -v xvfb-run >/dev/null 2>&1; then
        xvfb-run -a -s "-screen 0 1280x900x24" "$@"
    else
        "$@"
    fi
}

log "Building Skin Designer common (mvn -DskipTests install)"
( cd "$SKIN_DESIGNER_DIR" && mvn_with_display \
    mvn -B -ntp -DskipTests install -pl common -am )

SCREENSHOT_MAIN="com.codename1.tools.skindesigner.screenshots.ScreenshotApp"
log "Capturing screenshots via $SCREENSHOT_MAIN -> $OUT_DIR"
( cd "$SKIN_DESIGNER_DIR/common" && mvn_with_display \
    mvn -B -ntp -DskipTests exec:java \
        "-Dexec.mainClass=$SCREENSHOT_MAIN" \
        "-Dexec.args=$OUT_DIR" \
        -Dexec.classpathScope=compile )

log "Verifying captured PNGs"
missing=0
for name in "${SCREENSHOT_NAMES[@]}"; do
    src="$OUT_DIR/$name.png"
    if [ -f "$src" ]; then
        log "  ok: $src"
    else
        log "  ! missing: $src"
        missing=$((missing + 1))
    fi
done

if [ "$missing" -gt 0 ]; then
    log "$missing screenshot(s) missing; check the simulator log above for errors"
    exit 1
fi
log "Done"
