#!/usr/bin/env bash
# Builds the standalone native-reference screenshot app (scripts/fidelity-app/
# ios-native-ref/NativeRef.swift), runs it on the iOS simulator, and copies the
# real native-widget screenshots it produces into the committed iOS fidelity
# goldens. The native references are generated OFFLINE here (not regenerated each
# CI run): a real UIWindow renders the UIKit widgets correctly (unlike the
# off-screen factory render), and the CN1 fidelity suite then only renders the
# CN1 side and diffs it against these committed goldens.
#
# Usage: build-ios-native-ref.sh [simulator_udid]
set -euo pipefail

UDID="${1:-17853196-A8A7-45F2-8F06-24E8257945E6}"
BUNDLE_ID="com.codenameone.fidelity.nativeref"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/scripts/fidelity-app/ios-native-ref/NativeRef.swift"
# The golden SET this capture belongs to: the OS design generation of the
# booted simulator (ios-26-metal). Capturing iOS 27 references later = boot a
# 27 runtime and run with CN1SS_FIDELITY_GOLDEN_SET=ios-27-metal; the 26 set
# stays untouched, so both looks can be gated side by side during a phased
# migration.
GOLDEN_SET="${CN1SS_FIDELITY_GOLDEN_SET:-ios-26-metal}"
GOLDENS="$ROOT/scripts/fidelity-app/goldens/$GOLDEN_SET"
mkdir -p "$GOLDENS"
BUILD="$(mktemp -d)/NativeRef.app"
XCODE_APP="${XCODE_APP:-/Applications/Xcode.app}"
DEV="$XCODE_APP/Contents/Developer"

log() { echo "[native-ref] $*"; }

SDK="$(xcrun --sdk iphonesimulator --show-sdk-path)"
log "Compiling NativeRef.swift (sdk=$SDK)"
mkdir -p "$BUILD"
xcrun -sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    -sdk "$SDK" \
    -framework UIKit -parse-as-library \
    "$SRC" -o "$BUILD/NativeRef"

cat > "$BUILD/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleDevelopmentRegion</key><string>en</string>
  <key>CFBundleExecutable</key><string>NativeRef</string>
  <key>CFBundleIdentifier</key><string>${BUNDLE_ID}</string>
  <key>CFBundleInfoDictionaryVersion</key><string>6.0</string>
  <key>CFBundleName</key><string>NativeRef</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleShortVersionString</key><string>1.0</string>
  <key>CFBundleVersion</key><string>1</string>
  <key>LSRequiresIPhoneOS</key><true/>
  <key>MinimumOSVersion</key><string>15.0</string>
  <key>UIDeviceFamily</key><array><integer>1</integer></array>
  <key>UILaunchScreen</key><dict/>
  <key>DTPlatformName</key><string>iphonesimulator</string>
</dict>
</plist>
PLIST

# Bundle the shared glass backdrop so the app can render glass widgets over it.
cp "$ROOT/scripts/fidelity-app/common/src/main/resources/glass-backdrop.png" "$BUILD/" 2>/dev/null || true

log "Booting simulator $UDID"
xcrun simctl boot "$UDID" 2>/dev/null || true
xcrun simctl bootstatus "$UDID" -b >/dev/null 2>&1 || true

log "Installing native-ref app"
xcrun simctl uninstall "$UDID" "$BUNDLE_ID" 2>/dev/null || true
xcrun simctl install "$UDID" "$BUILD"

# Build+install only (record-ios-native-anim.sh launches the app itself in
# its animate mode instead of the default still-capture run).
if [ "${NATIVEREF_BUILD_ONLY:-0}" = "1" ]; then
  log "NATIVEREF_BUILD_ONLY=1 -- app installed, skipping still capture"
  exit 0
fi

log "Launching native-ref app"
# --console streams stdout until the app exits (it calls exit(0) after writing).
xcrun simctl launch --console-pty "$UDID" "$BUNDLE_ID" 2>&1 | sed 's/^/[native-ref][app] /' || true

CONTAINER="$(xcrun simctl get_app_container "$UDID" "$BUNDLE_ID" data 2>/dev/null || true)"
if [ -z "$CONTAINER" ] || [ ! -d "$CONTAINER/Documents" ]; then
    log "ERROR: could not locate app Documents container"
    exit 3
fi
N=$(ls "$CONTAINER/Documents/"*.png 2>/dev/null | wc -l | tr -d ' ')
log "Native screenshots produced: $N"
[ "$N" -gt 0 ] || { log "ERROR: no screenshots produced"; exit 4; }

mkdir -p "$GOLDENS"
cp "$CONTAINER/Documents/"*.png "$GOLDENS/"
log "Copied $N golden(s) -> $GOLDENS"
ls "$GOLDENS"/*.png | head -3 | sed 's/^/[native-ref]   /'
