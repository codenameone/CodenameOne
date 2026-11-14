#!/usr/bin/env bash
# Generates a sample application's Xcode project and builds it for the iOS
# simulator, unsigned. One call per sample; samples-ios-nightly.yml runs several
# in one macOS job so they share a warm runner (Maven repository, the restored
# CN1 + iOS port tree, CocoaPods, XcodeGen) instead of each taking a runner of
# its own -- macOS concurrency, not build time, is what the nightly waited on.
#
# Usage: scripts/ci/build-sample-ios-simulator.sh scripts/<sample> [--watch]
#
# --watch is for a sample with an embedded watch app: it drops -sdk, exactly as
# that sample's own PR job does (see below). Without it the build is the same
# `-sdk iphonesimulator` build the other samples' PR jobs run, so the nightly
# and the pull-request leg build the same thing.
set -euo pipefail

APP_DIR="${1:?usage: $0 scripts/<sample> [--watch]}"
WATCH="${2:-}"
NAME="$(basename "$APP_DIR")"
SCRATCH="${RUNNER_TEMP:-${TMPDIR:-/tmp}}"
OUT="$(mktemp "$SCRATCH/ios-sample-$NAME-out.XXXXXX")"
DERIVED_DATA="$SCRATCH/$NAME-dd"

# Several samples build in turn on one runner. Whatever this one leaves behind
# -- its derived data, its translated Xcode project and Pods -- is never read
# again, and five apps' worth (two with watch apps) can fill a runner's disk,
# failing every later sample for a reason that is not theirs. Clean up on
# failure too.
cleanup() {
  rm -rf "$DERIVED_DATA" "$APP_DIR/ios/target"
}
trap cleanup EXIT

# build-ios-app.sh writes its stats and logs to ARTIFACTS_DIR, by default one
# directory for the whole checkout; keep each sample's apart.
export ARTIFACTS_DIR="$SCRATCH/artifacts-$NAME"

# build-ios-app.sh reports the workspace and scheme through GITHUB_OUTPUT;
# point it at a file of this call's own so samples cannot read each other's.
CN1_APP_DIR="$APP_DIR" GITHUB_OUTPUT="$OUT" ./scripts/build-ios-app.sh -q -DskipTests
WORKSPACE="$(sed -n 's/^workspace=//p' "$OUT" | tail -n 1)"
SCHEME="$(sed -n 's/^scheme=//p' "$OUT" | tail -n 1)"
if [ -z "$WORKSPACE" ] || [ -z "$SCHEME" ]; then
  echo "::error::build-ios-app.sh reported no workspace/scheme for $APP_DIR"
  exit 1
fi

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
# With a watch app, no -sdk: it forces every target of the scheme onto the
# iPhone SDK, the embedded watch app included, whose watch-only icon then has
# nothing to compile for. The destination picks each target's own SDK.
SDK_ARGS=(-sdk iphonesimulator)
if [ "$WATCH" = "--watch" ]; then
  SDK_ARGS=()
fi
xcodebuild "$XCB_CONTAINER_FLAG" "$WORKSPACE" -scheme "$SCHEME" \
  ${SDK_ARGS[@]+"${SDK_ARGS[@]}"} \
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
