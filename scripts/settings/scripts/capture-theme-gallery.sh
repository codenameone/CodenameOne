#!/usr/bin/env bash
# Renders the Codename One Settings app under each desktop native theme, in light and
# dark, for the developer guide's Native Themes chapter.
#
#   scripts/settings/scripts/capture-theme-gallery.sh [output-dir] [theme ...]
#
# Output defaults to docs/developer-guide/img and the themes to "fluent aqua adwaita";
# each theme writes desktop-theme-settings-<theme>-{light,dark}.png. Build the app first
# (JDK 17):
#
#   cd scripts/settings
#   mvn install -DskipTests -Pjavase,executable-jar -Dcodename1.platform=javase
#
# Needs a real display -- never java.awt.headless -- so on Linux run it under xvfb-run.
#
# Where to capture each theme. The themes choose the platform's own typeface (Segoe UI
# Variable, SF or Cantarell) and fall back to a generic sans-serif where that face is
# not installed. Colors, borders and geometry come from the theme and are the same on
# any host, but a faithful image of a theme needs its own face:
#   fluent  -> Windows 11
#   aqua    -> macOS
#   adwaita -> Linux with Cantarell installed (fonts-cantarell)
# Pass the one theme a host can render faithfully, e.g. "... img aqua" on a Mac.
#
# The capture is the offscreen paint of the app's content pane, so the host's window
# frame and title bar never reach the image. The page shown is Basic, which carries
# fields, a switch, the rail, the brand Save button and a card.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SETTINGS_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SETTINGS_DIR/../.." && pwd)"
OUT_DIR="${1:-$REPO_ROOT/docs/developer-guide/img}"
shift || true
THEMES=("$@")
if [ ${#THEMES[@]} -eq 0 ]; then
  THEMES=(fluent aqua adwaita)
fi
# Half the stub's 2940x1224 Retina capture: the window's logical size, sharp at 45%
# scaled width in the guide's two-up layout.
WIDTH="${GALLERY_WIDTH:-1470}"

log() { echo "[capture-theme-gallery] $*"; }

JAVA_BIN="${JAVA17_HOME:-${JAVA_HOME:-}}"
JAVA_BIN="${JAVA_BIN:+$JAVA_BIN/bin/}java"
JAVASE_DIR="$SETTINGS_DIR/javase"
if [ ! -d "$JAVASE_DIR/target/classes" ] || [ ! -d "$JAVASE_DIR/target/libs" ]; then
  log "Settings is not built; see the build command at the top of this script." >&2
  exit 2
fi
CP="$JAVASE_DIR/target/classes"
for jar in "$JAVASE_DIR"/target/libs/*.jar; do
  CP="$CP:$jar"
done

# A throwaway project, named like an application so the chrome reads naturally. Under
# /tmp rather than $TMPDIR because its path is printed in the app's toolbar, and a macOS
# per-user temp directory fills that chip with /var/folders noise.
WORK="$(mktemp -d /tmp/cn1-theme-gallery.XXXXXX)"
trap 'rm -rf "$WORK"' EXIT
PROJECT="$WORK/FieldNotes"
mkdir -p "$PROJECT"
cat > "$PROJECT/codenameone_settings.properties" <<'EOF'
codename1.displayName=Field Notes
codename1.mainName=FieldNotes
codename1.packageName=com.example.fieldnotes
codename1.vendor=Example Inc
codename1.version=1.4.2
codename1.icon=icon.png
codename1.cssTheme=true
codename1.languageLevel=5
codename1.arg.java.version=17
codename1.arg.desktop.themeMode=native
EOF
printf '<project><modelVersion>4.0.0</modelVersion><groupId>com.example</groupId><artifactId>fieldnotes</artifactId><version>1.0</version></project>\n' \
  > "$PROJECT/pom.xml"
cat > "$WORK/binding.input" <<EOF
projectDir=$PROJECT
settings=$PROJECT/codenameone_settings.properties
pom=$PROJECT/pom.xml
multimoduleRoot=$PROJECT
EOF

downscale() {
  local file="$1"
  if command -v magick >/dev/null 2>&1; then
    magick "$file" -resize "${WIDTH}x" "$file"
  elif command -v convert >/dev/null 2>&1; then
    convert "$file" -resize "${WIDTH}x" "$file"
  elif command -v sips >/dev/null 2>&1; then
    sips --resampleWidth "$WIDTH" "$file" >/dev/null
  else
    log "no image tool (magick, convert or sips); leaving $file at capture size"
  fi
}

mkdir -p "$OUT_DIR"
failures=0
for theme in "${THEMES[@]}"; do
  for appearance in light dark; do
    dark=false
    [ "$appearance" = dark ] && dark=true
    out="$OUT_DIR/desktop-theme-settings-$theme-$appearance.png"
    diag="$WORK/$theme-$appearance.txt"
    rm -f "$out"
    log "$theme / $appearance -> $out"
    # settings.darkMode overrides both the saved preference and the OS appearance.
    "$JAVA_BIN" \
      -Dcodename1.arg.desktop.themeMode="$theme" \
      -Dsettings.darkMode="$dark" \
      -Dsettings.section=BASIC \
      -Dsettings.input="$WORK/binding.input" \
      -Dsettings.screenshot="$out" \
      -Dsettings.screenshot.onscreen=false \
      -Dsettings.screenshot.delay=3000 \
      -Dsettings.diagnostics="$diag" \
      -cp "$CP" com.codename1.settings.CodenameOneSettingsLauncher \
      > "$WORK/$theme-$appearance.log" 2>&1 || true
    # The launcher exits 0 even when the render went wrong, so check what it produced:
    # a file, in the appearance that was asked for.
    if [ ! -s "$out" ]; then
      log "  FAILED: no capture; launcher log follows" >&2
      cat "$WORK/$theme-$appearance.log" >&2
      failures=$((failures + 1))
      continue
    fi
    if ! grep -q "^display.darkMode=$dark\$" "$diag"; then
      log "  FAILED: rendered with the wrong appearance ($(grep '^display.darkMode' "$diag"))" >&2
      failures=$((failures + 1))
      continue
    fi
    downscale "$out"
  done
done
if [ "$failures" -gt 0 ]; then
  log "$failures capture(s) failed" >&2
  exit 1
fi
log "done"
