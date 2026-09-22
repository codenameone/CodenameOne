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
# JavaScriptPort's runtime serves themes out of its webapp assets folder;
# mirror the generated .res files there too so a local JS port run picks them
# up. The mirror is gitignored - Themes/ is the single source of truth, and
# the port poms (maven/ios, maven/android) consume it directly at build time.
JS_ASSETS_DIR="$REPO_ROOT/Ports/JavaScriptPort/src/main/webapp/assets"

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
  # A jar in ~/.m2 was built by some other checkout at some other commit, and
  # nothing here can tell which. Compiling with it re-emits the WHOLE theme the
  # way that build did, not just the rules you edited, so the committed .res
  # picks up differences no CSS diff can explain -- which is how a theme change
  # once moved screens that use none of its UIIDs. CI has no ~/.m2 and always
  # builds from source, so target/ above is the jar that matches the tree.
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
    case "$jar" in
      "$HOME"/.m2/*)
        log "WARNING: that jar comes from ~/.m2 and was built by another checkout."
        log "WARNING: it re-emits the whole theme its own way. Build the module first"
        log "WARNING: (mvn -f maven/css-compiler/pom.xml package) to compile with this tree."
        ;;
    esac
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

# Committed consumer copies of each generated .res. Themes/ is the canonical
# source the port poms read at build time, but several builds bundle their own
# checked-in copy and historically these drifted out of sync with theme.css
# (the fidelity CI bundles scripts/fidelity-app/.../resources, the ports stage
# nativeSources/Ports). Listing them here makes theme.css the single source of
# truth: one run of this script refreshes every committed copy so a stale .res
# can never silently regress the rendered theme. Paths are relative to REPO_ROOT.
ios_modern_copies() {
  # The fidelity-app copy is NOT listed: its common/pom.xml copies the .res
  # from Themes/ at build time (process-resources), so Themes/ stays the only
  # committed binary. Only the port staging copy remains a checked-in mirror.
  printf '%s\n' \
    "Ports/iOSPort/nativeSources/iOSModernTheme.res"
}
# "${name//-/_}_copies" -- ios-modern-27 resolves here.
ios_modern_27_copies() {
  printf '%s\n' \
    "Ports/iOSPort/nativeSources/iOSModern27Theme.res"
}
android_material_copies() {
  # See ios_modern_copies: the fidelity-app copy is build-time, not committed.
  printf '%s\n' \
    "Ports/Android/src/AndroidMaterialTheme.res"
}

# The desktop themes have NO committed consumer copies. Every desktop consumer
# (maven/javase, maven/windows, maven/linux, maven/mac) stages straight out of Themes/ at
# build time, so there is no second binary to drift out of sync -- which is the problem the
# copies above exist to solve for the ports that do embed one.
gnome_adwaita_copies() { :; }
windows_fluent_copies() { :; }
macos_aqua_copies() { :; }

# Every file a run WRITES, one repo-relative path per line, for callers that have to
# stage them -- .github/workflows/native-themes-sync.yml above all. That workflow kept a
# hand-written list of two paths, and it stayed at two when three desktop themes and two
# committed port mirrors were added: a desktop-only CSS change regenerated three .res
# files, staged none of them, reported "nothing to commit" and left every downstream
# build on the previous binary. A list the script produces cannot go stale that way.
#
# Off unless the caller asks for it, so a developer running this by hand writes nothing
# extra.
record_output() {
  [ -n "${NATIVE_THEMES_MANIFEST:-}" ] || return 0
  printf '%s\n' "$1" >> "$NATIVE_THEMES_MANIFEST"
}

# @import is a SILENT no-op. CSSTheme's importStyle (see
# maven/css-compiler/.../CSSTheme.java) has an EMPTY body: Flute parses the
# at-rule, the compiler ignores it, and every rule in the imported file vanishes
# from the .res with no error and no warning. Composition here is CONCATENATION
# (see theme_parts), so an @import is always a bug and always a silent one.
assert_no_import() {
  local hits
  hits="$(grep -rn --include='*.css' -E '^[[:space:]]*@import' "$CSS_SRC_ROOT" 2>/dev/null || true)"
  if [ -n "$hits" ]; then
    log "FAILED: @import is accepted by the CSS compiler and then ignored, so the"
    log "        imported rules would be missing from the theme with no diagnostic."
    log "        List the file in theme_parts() instead."
    printf '%s\n' "$hits" >&2
    exit 1
  fi
}

# The CSS files a variant is built from, in CASCADE ORDER, repo-relative.
#
# A variant is a LOGICAL name, not necessarily a directory: ios-modern and
# ios-modern-27 share every rule in ios-modern/common.css and differ only by
# which generation layer is appended. Everything else is still one self-contained
# theme.css and resolves through the default arm unchanged.
theme_parts() {
  case "$1" in
    ios-modern)    printf '%s\n' ios-modern/common.css ios-modern/gen26.css ;;
    ios-modern-27) printf '%s\n' ios-modern/common.css ios-modern/gen27.css ;;
    *)             printf '%s\n' "$1/theme.css" ;;
  esac
}

compile_theme() {
  local jar="$1" name="$2" basename="$3"
  local out="$OUT_DIR/$basename"
  local primary build_dir css part missing=0
  primary="$(theme_parts "$name" | head -n1)"; primary="${primary%%/*}"
  # The intermediate hangs off the theme's own directory rather than a mktemp,
  # because CSSTheme.getResourceImage resolves a `res/` image directory THREE
  # levels up from the input file. Under native-themes/<theme>/target/ that
  # still lands on native-themes/<theme>/res, exactly where it lands today.
  build_dir="$CSS_SRC_ROOT/$primary/target"
  css="$build_dir/$name.css"
  while IFS= read -r part; do
    if [ ! -f "$CSS_SRC_ROOT/$part" ]; then
      log "Skipping $name: no source at $part"
      missing=1
    fi
  done < <(theme_parts "$name")
  [ "$missing" -eq 0 ] || return
  mkdir -p "$OUT_DIR" "$build_dir"
  : > "$css"
  while IFS= read -r part; do
    # A provenance banner per part: parse errors report a line number in THIS
    # concatenated file, so without it an error in gen27.css points at a line of
    # a file nobody edited. Comments are inert here -- the dark-mode rewriter
    # uses indexOfOutsideComments and skips them.
    printf '/* ===== native-themes/%s ===== */\n' "$part" >> "$css"
    cat "$CSS_SRC_ROOT/$part" >> "$css"
    printf '\n' >> "$css"
  done < <(theme_parts "$name")
  log "Compiling $name -> $out"
  java -jar "$jar" -input "$css" -output "$out"
  record_output "${out#"$REPO_ROOT"/}"
  if [ -d "$JS_ASSETS_DIR" ]; then
    cp "$out" "$JS_ASSETS_DIR/$basename"
    log "Mirrored -> $JS_ASSETS_DIR/$basename"
    record_output "${JS_ASSETS_DIR#"$REPO_ROOT"/}/$basename"
  fi
  local copy
  while IFS= read -r copy; do
    [ -n "$copy" ] || continue
    mkdir -p "$REPO_ROOT/$(dirname "$copy")"
    cp "$out" "$REPO_ROOT/$copy"
    log "Mirrored -> $copy"
    record_output "$copy"
  done < <("${name//-/_}_copies")
}

main() {
  if [ -n "${NATIVE_THEMES_MANIFEST:-}" ]; then
    : > "$NATIVE_THEMES_MANIFEST"
  fi
  local jar
  assert_no_import
  jar="$(ensure_jar)"
  compile_theme "$jar" ios-modern iOSModernTheme.res
  compile_theme "$jar" ios-modern-27 iOSModern27Theme.res
  compile_theme "$jar" android-material AndroidMaterialTheme.res
  compile_theme "$jar" gnome-adwaita GnomeAdwaitaTheme.res
  compile_theme "$jar" windows-fluent WindowsFluentTheme.res
  compile_theme "$jar" macos-aqua MacOSAquaTheme.res
  log "Native themes written to $OUT_DIR/ and committed consumer copies"
}

main "$@"
