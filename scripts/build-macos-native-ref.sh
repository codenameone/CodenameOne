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

# Pin what must not drift between runs. The app reads all of these at launch, so writing
# them here and then launching a fresh process is enough -- the cfprefsd caching problem
# only affects processes that are already running.
defaults write -g NSAutomaticWindowAnimationsEnabled -bool false || true
defaults write -g AppleFontSmoothing -int 0 || true
defaults write -g AppleShowScrollBars -string Always || true

# The GitHub macOS runner ships with Reduce Transparency ON (measured: run 34936153808
# reported reduce_transparency true on macos-15). That flattens every vibrancy and material
# surface to an opaque fill, which is the macOS counterpart of Mica falling back on Windows
# Server -- capture under it and the reference silently encodes a design nobody sees.
# Turned off here; the app still asserts it afterwards, so if the write does not take the
# run fails rather than producing a flat reference.
defaults write com.apple.universalaccess reduceTransparency -bool false || true
defaults write com.apple.universalaccess increaseContrast -bool false || true

# Delete rather than set the accent: absent means "the system default", which is the
# reference we want. Setting a value would encode a choice no default Mac has made.
defaults delete -g AppleAccentColor 2>/dev/null || true
defaults delete -g AppleHighlightColor 2>/dev/null || true

export NATIVEREF_OUT="$OUT_DIR"
export NATIVEREF_MODE="${NATIVEREF_MODE:-probe}"

log "Running (mode=$NATIVEREF_MODE)"

# Launched through LaunchServices, not exec'd. Running the binary directly from a
# non-interactive shell -- which is what a CI runner gives you -- starts the app outside any
# Aqua session, so it never becomes active and every AppKit control renders in its
# inactive, greyed style. The first probe run measured exactly that: key=false main=false
# appActive=false. scripts/run-macos-ui-tests.sh already documents this for the macOS port
# and solves it the same way; `open -W -n` gets the app the session a user-launched Mac app
# would have.
#
# The cost is that stdout is no longer our pipe and `open` does not propagate the app's
# exit status, so the log is redirected to a file and the status is read back out of it.
TEST_LOG="$BUILD/nativeref.log"
: > "$TEST_LOG"

# caffeinate: a sleeping display makes every screen-capture API return black. It costs
# nothing when the display is already awake.
set +e
caffeinate -dimsu -w $$ &
CAFF=$!
open -W -n -F \
     --stdout "$TEST_LOG" \
     --stderr "$TEST_LOG" \
     --env "NATIVEREF_OUT=$NATIVEREF_OUT" \
     --env "NATIVEREF_MODE=$NATIVEREF_MODE" \
     --env "CN1SS_FIDELITY_GOLDEN_SET=${CN1SS_FIDELITY_GOLDEN_SET:-macos-aqua}" \
     -a "$APP"
kill "$CAFF" 2>/dev/null
set -e

cat "$TEST_LOG"

# `open -W` exits 0 whatever the app did, so the app reports its own verdict on the last
# line and it is read back here. A missing verdict means the app died before finishing,
# which must fail rather than pass quietly.
# Tolerant of extra fields before exit= (the capture run reports tiles= as well), but
# still anchored to the whole line so a half-written log cannot satisfy it.
rc=$(sed -n 's/^NATIVEREF:DONE .*exit=\([0-9][0-9]*\)$/\1/p' "$TEST_LOG" | tail -1)
if [ -z "$rc" ]; then
  log "FAILED: the app never reported NATIVEREF:DONE -- it exited before finishing."
  exit 22
fi

if [ "$rc" -ne 0 ]; then
  log "FAILED: the reference app exited $rc; see the BLOCKER lines above."
  log "A blocker means the capture would have been quietly wrong, not that it crashed."
  exit "$rc"
fi
log "Wrote $(ls -1 "$OUT_DIR" | wc -l | tr -d ' ') file(s) to $OUT_DIR"
