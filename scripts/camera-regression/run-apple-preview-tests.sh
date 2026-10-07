#!/usr/bin/env bash
# Local native layout test; optional argument is a booted iOS Simulator UDID.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMP="$(mktemp -d "${TMPDIR:-/tmp}/cn1-preview.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT
python3 - "$ROOT" "$TMP" <<'PY'
from pathlib import Path
import sys
root, target = map(Path, sys.argv[1:])
s = (root / 'Ports/iOSPort/nativeSources/CN1Camera.m').read_text()
assert '[[CN1CameraPreviewView alloc]' in s, 'Camera must use the tested preview view'
start = s.index('@interface CN1CameraPreviewView')
end = s.index('@end', s.index('@implementation CN1CameraPreviewView')) + len('@end')
(target / 'PreviewProduction.inc').write_text(s[start:end])
PY
SRC="$ROOT/scripts/camera-regression/apple/PreviewLayoutTest.m"
# Check both the manual ownership build and the ARC build.
for OWNERSHIP in -fno-objc-arc -fobjc-arc; do
    xcrun --sdk macosx clang "$OWNERSHIP" -Werror -Wno-deprecated-declarations \
        -I "$TMP" -framework AppKit -framework AVFoundation -framework QuartzCore -framework CoreGraphics \
        "$SRC" -o "$TMP/preview-test"
    "$TMP/preview-test"
done
if [ -n "${1:-}" ]; then
    APP="$TMP/CameraPreviewTest.app"
    mkdir -p "$APP"
    xcrun --sdk iphonesimulator clang -fobjc-arc -Werror -Wno-deprecated-declarations \
        -target arm64-apple-ios15.0-simulator \
        -isysroot "$(xcrun --sdk iphonesimulator --show-sdk-path)" \
        -I "$TMP" -framework UIKit -framework AVFoundation -framework QuartzCore -framework CoreGraphics \
        "$SRC" -o "$APP/CameraPreviewTest"
    cat > "$APP/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleIdentifier</key><string>com.codename1.camera.previewregression</string>
<key>CFBundleName</key><string>CameraPreviewTest</string>
<key>CFBundleExecutable</key><string>CameraPreviewTest</string>
<key>CFBundleVersion</key><string>1</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>UILaunchScreen</key><dict/>
<key>UIApplicationSceneManifest</key><dict>
<key>UIApplicationSupportsMultipleScenes</key><false/>
<key>UISceneConfigurations</key><dict/></dict>
</dict></plist>
PLIST
    codesign --force --sign - "$APP"
    xcrun simctl install "$1" "$APP"
    xcrun simctl launch --console "$1" com.codename1.camera.previewregression | tee "$TMP/simulator.log"
    grep -q 'PASS: camera preview' "$TMP/simulator.log"
fi
