#!/usr/bin/env bash
###
# Compile the shipped platform native themes from CSS source.
#
# Uses the thin codenameone-css-compiler jar (no JavaFX / no CEF, depends only
# on codenameone-core + flute + sac) so it runs fast and fails loudly if any
# CSS rule would require CEF-backed rasterization (box-shadow, border-radius
# with visible border, filter, complex gradients).
#
# Source layout:
#   native-themes/
#     ios-modern/theme.css
#     android-material/theme.css
#   (see native-themes/README for authoring rules)
#
# Outputs land in the existing Themes/ directory next to the hand-authored
# legacy themes, and are picked up by each port's build.xml the same way the
# legacy .res files are today. Outputs are gitignored (build artifacts).
###
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$REPO_ROOT"

log() { echo "[build-native-themes] $1" >&2; }

CSS_COMPILER_MODULE="$REPO_ROOT/maven/css-compiler"
CSS_SRC_ROOT="$REPO_ROOT/native-themes"
OUT_DIR="$REPO_ROOT/Themes"
# Every committed copy of the .res files that downstream consumers ship from.
# Keeping these locations in sync here means the native-themes-sync workflow
# can `git add` the whole set after running this script.
JS_ASSETS_DIR="$REPO_ROOT/Ports/JavaScriptPort/src/main/webapp/assets"
IOS_NATIVE_DIR="$REPO_ROOT/Ports/iOSPort/nativeSources"
ANDROID_SRC_DIR="$REPO_ROOT/Ports/Android/src"

# Resolve the compiler jar. Prefer a freshly-built target/ jar (so CSS compiler
# source edits are always picked up); fall back to the installed copy in ~/.m2
# when the module hasn't been rebuilt in this session.
locate_jar() {
  local target_jar installed_jar version
  target_jar="$(ls "$CSS_COMPILER_MODULE"/target/codenameone-css-compiler-*-jar-with-dependencies.jar 2>/dev/null | head -n1 || true)"
  if [ -n "$target_jar" ] && [ -f "$target_jar" ]; then
    echo "$target_jar"
    return
  fi
  version="$(grep -m1 '<version>' "$CSS_COMPILER_MODULE/pom.xml" | sed -E 's#.*<version>([^<]+)</version>.*#\1#')"
  if [ -z "$version" ]; then
    # Fall back to parent pom version if the module inherits it.
    version="$(grep -m1 '<version>' "$REPO_ROOT/maven/pom.xml" | sed -E 's#.*<version>([^<]+)</version>.*#\1#')"
  fi
  installed_jar="$HOME/.m2/repository/com/codenameone/codenameone-css-compiler/$version/codenameone-css-compiler-${version}-jar-with-dependencies.jar"
  if [ -f "$installed_jar" ]; then
    echo "$installed_jar"
    return
  fi
  return 1
}

ensure_jar() {
  local jar
  if jar="$(locate_jar)"; then
    log "Using CSS compiler jar: $jar"
    printf '%s\n' "$jar"
    return
  fi
  log "CSS compiler jar not found; building it via Maven."
  local mvn="${MAVEN_HOME:+$MAVEN_HOME/bin/mvn}"
  mvn="${mvn:-mvn}"
  # Redirect Maven output to stderr - otherwise its stdout gets captured
  # by the calling `jar="$(ensure_jar)"` and ends up concatenated with
  # the jar path, which `java -jar` then chokes on. The parent pom
  # initialise antrun also clones cn1-binaries with failonerror unset,
  # so a benign `[ERROR] [exec] Result: 128` (already-exists clone)
  # would pollute stdout if we let it through.
  (
    cd "$REPO_ROOT/maven"
    "$mvn" -pl css-compiler -am -q -DskipTests install
  ) >&2
  if jar="$(locate_jar)"; then
    printf '%s\n' "$jar"
    return
  fi
  log "FAILED: CSS compiler jar could not be located after build."
  exit 1
}

compile_theme() {
  local jar="$1" name="$2" basename="$3"
  shift 3
  local css="$CSS_SRC_ROOT/$name/theme.css"
  local out="$OUT_DIR/$basename"
  if [ ! -f "$css" ]; then
    log "Skipping $name: no source at $css"
    return
  fi
  mkdir -p "$OUT_DIR"
  log "Compiling $name -> $out"
  java -jar "$jar" -input "$css" -output "$out"
  local mirror
  for mirror in "$@"; do
    [ -d "$mirror" ] || mkdir -p "$mirror"
    cp "$out" "$mirror/$basename"
    log "Mirrored -> $mirror/$basename"
  done
}

main() {
  local jar
  jar="$(ensure_jar)"
  compile_theme "$jar" ios-modern iOSModernTheme.res \
    "$JS_ASSETS_DIR" "$IOS_NATIVE_DIR"
  compile_theme "$jar" android-material AndroidMaterialTheme.res \
    "$JS_ASSETS_DIR" "$ANDROID_SRC_DIR"
  log "Native themes written to $OUT_DIR/"
}

main "$@"
