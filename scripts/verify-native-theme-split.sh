#!/usr/bin/env bash
###
# Prove that splitting native-themes/ios-modern/theme.css into
# common.css + gen26.css changed NOTHING about generation 26.
#
# The iOS Modern theme is the default for every app that asks for the modern
# look, so the split has to be a no-op for it -- and "looks equivalent" is not
# a claim anyone can check. This turns it into one command.
#
# It is sound because the compiler is byte-reproducible on these inputs:
# CSSTheme names the resource "Theme" rather than the input path, NoCefCSSCLI
# never reads the existing .res, theme entries are sorted before writing
# (EditableResources), there are no timestamps, and none of the native themes
# reference an image or a font, so no encoder runs. Measured: recompiling the
# committed theme.css reproduced Themes/iOSModernTheme.res byte for byte.
#
# BOTH sides must be compiled with the SAME jar. A jar from ~/.m2 was built by
# another checkout at another commit and re-emits the whole theme its own way,
# which would show up here as a difference that has nothing to do with the
# split.
#
# Usage:
#   scripts/verify-native-theme-split.sh [baseline-git-ref]
#
# The ref defaults to the merge base with origin/master -- i.e. "the theme as it
# was before this branch touched it". Pass an explicit ref to compare against
# some other point.
###
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$REPO_ROOT"

log() { echo "[verify-native-theme-split] $1" >&2; }

BASE_REF="${1:-}"
if [ -z "$BASE_REF" ]; then
    BASE_REF="$(git merge-base HEAD origin/master 2>/dev/null || echo "")"
fi
if [ -z "$BASE_REF" ]; then
    log "Could not determine a baseline ref; pass one explicitly."
    exit 2
fi

# The theme was a single file before the split and is three after it. Look for
# whichever the baseline has, so this keeps working from either side.
BASE_CSS=""
for candidate in native-themes/ios-modern/theme.css native-themes/ios-modern/common.css; do
    if git cat-file -e "$BASE_REF:$candidate" 2>/dev/null; then
        BASE_CSS="$candidate"
        break
    fi
done
if [ -z "$BASE_CSS" ]; then
    log "No iOS Modern CSS found at $BASE_REF."
    exit 2
fi

WORK="$REPO_ROOT/native-themes/ios-modern/target"
mkdir -p "$WORK"

# Reuse the build script's own jar resolution so both compiles use one jar and
# the ~/.m2 warning is not duplicated here.
# shellcheck source=build-native-themes.sh
JAR="$(
    NATIVE_THEMES_JAR_ONLY=1 bash -c '
        set -euo pipefail
        source "'"$REPO_ROOT"'/scripts/build-native-themes.sh" >/dev/null 2>&1 || true
        ensure_jar
    ' 2>/dev/null | tail -n1
)"
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
    JAR="$(ls "$REPO_ROOT"/maven/css-compiler/target/codenameone-css-compiler-*-jar-with-dependencies.jar 2>/dev/null | head -n1 || true)"
fi
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
    log "No CSS compiler jar. Build it first:"
    log "  mvn -f maven/css-compiler/pom.xml -DskipTests package"
    exit 3
fi
log "Using CSS compiler jar: $JAR"

git show "$BASE_REF:$BASE_CSS" > "$WORK/baseline.css"
log "Baseline: $BASE_REF:$BASE_CSS"
java -jar "$JAR" -input "$WORK/baseline.css" -output "$WORK/baseline.res" >/dev/null

# The split side is whatever the build script would feed the compiler today.
cat native-themes/ios-modern/common.css > "$WORK/split.css"
printf '\n' >> "$WORK/split.css"
cat native-themes/ios-modern/gen26.css >> "$WORK/split.css"
java -jar "$JAR" -input "$WORK/split.css" -output "$WORK/split.res" >/dev/null

if cmp -s "$WORK/baseline.res" "$WORK/split.res"; then
    log "OK: common.css + gen26.css compiles byte-for-byte to the pre-split theme."
    exit 0
fi

log "FAILED: generation 26 is NOT what it was before the split."
log "  baseline: $WORK/baseline.res ($(wc -c < "$WORK/baseline.res") bytes)"
log "  split:    $WORK/split.res ($(wc -c < "$WORK/split.res") bytes)"
log "Anything moved out of common.css must land in gen26.css as well as gen27.css."
exit 1
