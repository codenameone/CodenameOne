#!/usr/bin/env bash
# Drive the Skin Designer through each wizard stage in the JavaSE simulator
# under xvfb, take a Robot screenshot per scenario, and write the PNGs into
# docs/developer-guide/img/skin-designer/.
#
# Run by .github/workflows/skin-designer-screenshots.yml on a schedule and
# on workflow_dispatch. Locally:
#
#     scripts/skindesigner/screenshots/take-screenshots.sh
#
# Requires: Java 17 + Maven on PATH. Linux: xvfb-run.
set -euo pipefail

log() { echo "[skin-designer-screenshots] $1" >&2; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKIN_DESIGNER_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKIN_DESIGNER_DIR/../.." && pwd)"

OUT_DIR="${OUT_DIR:-$REPO_ROOT/docs/developer-guide/img/skin-designer}"
mkdir -p "$OUT_DIR"

JAVA_BIN="${JAVA_BIN:-$(command -v java)}"
JAVAC_BIN="${JAVAC_BIN:-$(command -v javac)}"
if [ -z "$JAVA_BIN" ] || [ -z "$JAVAC_BIN" ]; then
    log "JDK not found on PATH"
    exit 2
fi

# 1. Build the Skin Designer Maven project so `target/classes` and the
#    cn1lib dependencies are populated.
log "Building Skin Designer (mvn -DskipTests install)"
(cd "$SKIN_DESIGNER_DIR" && mvn -B -ntp -DskipTests install)

# 2. Resolve the simulator classpath. The cn1 plugin's `cn1:run` goal
#    prints it; `mvn dependency:build-classpath` is more portable.
CP_FILE="$(mktemp)"
trap 'rm -f "$CP_FILE"' EXIT
log "Resolving runtime classpath"
(cd "$SKIN_DESIGNER_DIR/javase" && \
    mvn -B -ntp -q dependency:build-classpath \
        -Dmdep.outputFile="$CP_FILE" \
        -Dcodename1.platform=javase \
        -Pjavase >/dev/null)

SIM_CP="$(cat "$CP_FILE"):$SKIN_DESIGNER_DIR/common/target/classes:$SKIN_DESIGNER_DIR/javase/target/classes"

# 3. Compile the screenshot harness against the simulator classpath.
HARNESS_CLASSES="$(mktemp -d)"
trap 'rm -rf "$HARNESS_CLASSES"' EXIT
log "Compiling screenshot harness"
"$JAVAC_BIN" -d "$HARNESS_CLASSES" \
    -cp "$SIM_CP" \
    "$SCRIPT_DIR/lib/SkinDesignerScreenshotter.java"

HARNESS_CP="$HARNESS_CLASSES:$SIM_CP"

# 4. Run each scenario.
run_scenario() {
    local name="$1"; shift
    local out="$OUT_DIR/$name.png"
    log "Running scenario $name"
    local args=(--scenario "$name" --sim-classpath "$SIM_CP" --screenshot "$out")
    while [ "$#" -gt 0 ]; do
        args+=("$@")
        shift "$#"
    done
    if command -v xvfb-run >/dev/null 2>&1; then
        xvfb-run -a -s "-screen 0 1600x1100x24" "$JAVA_BIN" \
            -Djava.awt.headless=false \
            -cp "$HARNESS_CP" \
            com.codenameone.tools.skindesigner.screenshots.SkinDesignerScreenshotter \
            "${args[@]}"
    else
        "$JAVA_BIN" \
            -Djava.awt.headless=false \
            -cp "$HARNESS_CP" \
            com.codenameone.tools.skindesigner.screenshots.SkinDesignerScreenshotter \
            "${args[@]}"
    fi
    log "  -> $out"
}

# Scenarios mirror the developer-guide structure. The --demo overrides
# get forwarded as -Dcn1.skindesigner.* to the simulator JVM.
run_scenario skin-designer-stage-1-device \
    --demo cn1.skindesigner.demoStep=0 \
    --demo cn1.skindesigner.demoDevice=apple_apple_iphone_16_pro

run_scenario skin-designer-stage-2-source \
    --demo cn1.skindesigner.demoStep=1 \
    --demo cn1.skindesigner.demoDevice=apple_apple_iphone_16_pro

run_scenario skin-designer-stage-3-editor-shape \
    --demo cn1.skindesigner.demoStep=2 \
    --demo cn1.skindesigner.demoDevice=apple_apple_iphone_16_pro \
    --demo cn1.skindesigner.demoSource=shape \
    --demo cn1.skindesigner.demoPreset=island \
    --demo cn1.skindesigner.demoSidebarTab=shape

run_scenario skin-designer-stage-3-editor-cutouts \
    --demo cn1.skindesigner.demoStep=2 \
    --demo cn1.skindesigner.demoDevice=apple_apple_iphone_16_pro \
    --demo cn1.skindesigner.demoSource=shape \
    --demo cn1.skindesigner.demoPreset=island \
    --demo cn1.skindesigner.demoSidebarTab=cutouts

run_scenario skin-designer-stage-3-editor-info \
    --demo cn1.skindesigner.demoStep=2 \
    --demo cn1.skindesigner.demoDevice=apple_apple_iphone_16_pro \
    --demo cn1.skindesigner.demoSource=shape \
    --demo cn1.skindesigner.demoPreset=island \
    --demo cn1.skindesigner.demoSidebarTab=info

run_scenario skin-designer-stage-4-done \
    --demo cn1.skindesigner.demoStep=3 \
    --demo cn1.skindesigner.demoDevice=apple_apple_iphone_16_pro \
    --demo cn1.skindesigner.demoSource=shape \
    --demo cn1.skindesigner.demoPreset=island \
    --warmup 12

log "Done. Screenshots are in $OUT_DIR"
