#!/usr/bin/env bash
# Run the Skin Designer's ScreenshotApp Lifecycle inside the JavaSE
# simulator. Each scenario calls Display.captureScreen() and saves the
# PNG via Storage; on JavaSE that lands in ~/.cn1/. We then copy those
# PNGs into docs/developer-guide/img/skin-designer/.
#
# Run by .github/workflows/skin-designer-screenshots.yml on a schedule
# and on workflow_dispatch. Locally (Linux):
#
#     scripts/skindesigner/screenshots/take-screenshots.sh
#
# Requires Java 17 + Maven on PATH. Linux: xvfb-run for headless capture.
set -euo pipefail

log() { echo "[skin-designer-screenshots] $1" >&2; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKIN_DESIGNER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKIN_DESIGNER_DIR/../.." && pwd)"

OUT_DIR="${OUT_DIR:-$REPO_ROOT/docs/developer-guide/img/skin-designer}"
mkdir -p "$OUT_DIR"

# Names match the SCENARIOS array in ScreenshotApp.java; keep in sync.
SCREENSHOT_NAMES=(
    "skin-designer-stage-1-device"
    "skin-designer-stage-2-source"
    "skin-designer-stage-3-editor-shape"
    "skin-designer-stage-3-editor-cutouts"
    "skin-designer-stage-3-editor-info"
    "skin-designer-stage-4-done"
)

# Storage on the JavaSE port lives at ~/.cn1; the ScreenshotApp writes
# each scenario as <name>.png into Storage.
STORAGE_DIR="$HOME/.cn1"

# Wipe any stale screenshots from a previous run so a partial run can
# never report success with old files.
for name in "${SCREENSHOT_NAMES[@]}"; do
    rm -f "$STORAGE_DIR/$name.png"
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

log "Building Skin Designer (mvn -DskipTests install)"
(cd "$SKIN_DESIGNER_DIR" && mvn -B -ntp -DskipTests install)

# The CN1 Maven plugin composes codename1.mainClass from the package +
# mainName at startup. We override codename1.mainClass directly here so
# the simulator launches our ScreenshotApp instead of the regular
# SkinDesigner.
SCREENSHOT_MAIN="com.codename1.tools.skindesigner.screenshots.ScreenshotApp"
log "Running simulator with $SCREENSHOT_MAIN"
RUN_CMD=(mvn -B -ntp -Psimulator -DskipTests
         -Dcodename1.platform=javase
         "-Dcodename1.mainClass=$SCREENSHOT_MAIN"
         -f "$SKIN_DESIGNER_DIR/javase/pom.xml"
         verify)

if command -v xvfb-run >/dev/null 2>&1; then
    xvfb-run -a -s "-screen 0 1600x1100x24" "${RUN_CMD[@]}"
else
    "${RUN_CMD[@]}"
fi

log "Copying generated PNGs out of $STORAGE_DIR"
missing=0
for name in "${SCREENSHOT_NAMES[@]}"; do
    src="$STORAGE_DIR/$name.png"
    if [ -f "$src" ]; then
        cp "$src" "$OUT_DIR/$name.png"
        log "  -> $OUT_DIR/$name.png"
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
