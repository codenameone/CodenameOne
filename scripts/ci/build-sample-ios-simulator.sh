#!/usr/bin/env bash
# Generates a sample application's Xcode project and builds it for the iOS
# simulator, unsigned. One call per sample; samples-ios-nightly.yml runs several
# in one macOS job so they share a warm runner (Maven repository, the restored
# CN1 + iOS port tree, CocoaPods, XcodeGen) instead of each taking a runner of
# its own -- macOS concurrency, not build time, is what the nightly waited on.
#
# Usage: scripts/ci/build-sample-ios-simulator.sh scripts/<sample>
set -euo pipefail

APP_DIR="${1:?usage: $0 scripts/<sample>}"
NAME="$(basename "$APP_DIR")"
OUT="$(mktemp "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/ios-sample-$NAME-out.XXXXXX")"

# build-ios-app.sh reports the workspace and scheme through GITHUB_OUTPUT;
# point it at a file of this call's own so samples cannot read each other's.
CN1_APP_DIR="$APP_DIR" GITHUB_OUTPUT="$OUT" ./scripts/build-ios-app.sh -q -DskipTests
WORKSPACE="$(sed -n 's/^workspace=//p' "$OUT" | tail -n 1)"
SCHEME="$(sed -n 's/^scheme=//p' "$OUT" | tail -n 1)"
if [ -z "$WORKSPACE" ] || [ -z "$SCHEME" ]; then
  echo "::error::build-ios-app.sh reported no workspace/scheme for $APP_DIR"
  exit 1
fi

DERIVED_DATA="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/$NAME-dd"
rm -rf "$DERIVED_DATA"
# One architecture, the host's: the port's SIMD natives are NEON under
# __aarch64__, and a universal simulator build compiles an x86_64 slice that has
# no use here.
case "$(uname -m)" in
  arm64|x86_64) BUILD_ARCH="$(uname -m)" ;;
  *) BUILD_ARCH=arm64 ;;
esac
XCB_CONTAINER_FLAG="-workspace"
if [[ "$WORKSPACE" != *.xcworkspace ]]; then
  XCB_CONTAINER_FLAG="-project"
fi
# No -sdk: it forces every target of the scheme onto the iPhone SDK, an
# embedded watch app included (dash, pulse, splat, slate), whose watch-only icon
# then has nothing to compile for. The destination picks each target's own SDK,
# which for a plain app is iphonesimulator -- the same build -sdk asked for.
xcodebuild "$XCB_CONTAINER_FLAG" "$WORKSPACE" -scheme "$SCHEME" \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath "$DERIVED_DATA" \
  ARCHS="$BUILD_ARCH" ONLY_ACTIVE_ARCH=YES \
  EXCLUDED_ARCHS="armv7 armv7s" \
  CODE_SIGNING_ALLOWED=NO build

APP_BUNDLE="$(find "$DERIVED_DATA/Build/Products" -maxdepth 3 -type d -name "${SCHEME}.app" | head -n 1)"
if [ -z "$APP_BUNDLE" ]; then
  echo "::error::no .app bundle under $DERIVED_DATA for $APP_DIR"
  exit 1
fi
echo "Built $APP_BUNDLE"
# The derived data of a finished sample is not read again; free the disk for
# the next one on this runner.
rm -rf "$DERIVED_DATA"
