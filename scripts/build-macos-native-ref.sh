#!/usr/bin/env bash
###
# Build and run the macOS (AppKit) native reference app.
#
# Same shape as scripts/build-ios-native-ref.sh: one Swift file compiled with `xcrun
# swiftc`, a hand-written Info.plist, no xcodeproj. Unsigned is fine -- a locally built
# bundle carries no quarantine xattr, so Gatekeeper never sees it.
#
# Runs on a hosted CI runner rather than the maintainer's Mac, because a working
# developer's Mac has a chosen accent colour, a chosen appearance and custom fonts, and a
# reference captured there would encode all three. See
# .github/workflows/fidelity-desktop-native-ref.yml.
#
# Honours NATIVEREF_MODE (probe|capture) and CN1SS_FIDELITY_GOLDEN_SET.
###
set -euo pipefail

log() { echo "[build-macos-native-ref] $1" >&2; }

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$REPO_ROOT"

SRC="$REPO_ROOT/scripts/fidelity-app/macos-native-ref/NativeRef.swift"
OUT_DIR="$REPO_ROOT/artifacts/desktop-native-ref/macos"
mkdir -p "$OUT_DIR"

BUILD="$(mktemp -d "${TMPDIR:-/tmp}/cn1-macos-ref-XXXXXX")"
APP="$BUILD/NativeRef.app"
mkdir -p "$APP/Contents/MacOS"

SDK="$(xcrun --sdk macosx --show-sdk-path)"
ARCH="$(uname -m)"
log "Compiling for $ARCH against $SDK"
xcrun -sdk macosx swiftc \
    -target "${ARCH}-apple-macos13.0" \
    -sdk "$SDK" \
    -framework AppKit \
    -parse-as-library \
    "$SRC" -o "$APP/Contents/MacOS/NativeRef"

# LSUIElement is deliberately absent: the app must be .regular so it can become frontmost.
# An NSWindow that never becomes key draws every AppKit control in its inactive style.
cat > "$APP/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key><string>NativeRef</string>
    <key>CFBundleIdentifier</key><string>com.codenameone.fidelity.nativeref</string>
    <key>CFBundleExecutable</key><string>NativeRef</string>
    <key>CFBundlePackageType</key><string>APPL</string>
    <key>CFBundleShortVersionString</key><string>1.0</string>
    <key>NSPrincipalClass</key><string>NSApplication</string>
    <key>NSHighResolutionCapable</key><true/>
    <key>LSMinimumSystemVersion</key><string>13.0</string>
</dict>
</plist>
PLIST

# Pin what must not drift between runs. These are WRITES, unlike the accessibility settings
# the app asserts on: the app reads them at launch, so they take effect without a cfprefsd
# dance, and a runner image that changed one would otherwise silently change the reference.
defaults write -g NSAutomaticWindowAnimationsEnabled -bool false || true
defaults write -g AppleFontSmoothing -int 0 || true
defaults write -g AppleShowScrollBars -string Always || true
# Delete rather than set the accent: absent means "the system default", which is the
# reference we want. Setting a value would encode a choice no default Mac has made.
defaults delete -g AppleAccentColor 2>/dev/null || true
defaults delete -g AppleHighlightColor 2>/dev/null || true

export NATIVEREF_OUT="$OUT_DIR"
export NATIVEREF_MODE="${NATIVEREF_MODE:-probe}"

# The binary is exec'd directly rather than launched with `open`, so its stdout reaches the
# CI log. If a run reports app_active=false in the manifest, that is the macOS 14+
# focus-stealing restriction and the remedy is to launch with `open -W "$APP"` and have the
# app write its log to a file instead -- worth doing only if CI actually hits it, because
# exec'ing keeps the diagnostics inline where they are readable.
log "Running (mode=$NATIVEREF_MODE)"
# caffeinate: a sleeping display makes every screen-capture API return black, and this
# machine has been bitten by that before (screencapture -R silently returning solid black
# once the screen slept). It costs nothing when the display is already awake.
set +e
caffeinate -dimsu -w $$ &
CAFF=$!
"$APP/Contents/MacOS/NativeRef"
rc=$?
kill "$CAFF" 2>/dev/null
set -e

if [ "$rc" -ne 0 ]; then
  log "FAILED: the reference app exited $rc; see the BLOCKER lines above."
  log "A blocker means the capture would have been quietly wrong, not that it crashed."
  exit "$rc"
fi
log "Wrote $(ls -1 "$OUT_DIR" | wc -l | tr -d ' ') file(s) to $OUT_DIR"
