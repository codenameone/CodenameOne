#!/usr/bin/env bash
# Convenience wrapper to build the native-fidelity app for a platform by
# delegating to the existing per-platform CN1 build scripts with
# CN1_APP_DIR=scripts/fidelity-app. The fidelity app forces the iOS Metal
# pipeline and bundles the Material gradle dependency via its build hints.
#
# Usage: build-fidelity-app.sh <android|ios>
set -euo pipefail

PLATFORM="${1:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export CN1_APP_DIR="scripts/fidelity-app"

case "$PLATFORM" in
  android)
    exec "$SCRIPT_DIR/build-android-app.sh" "${@:2}"
    ;;
  ios)
    exec "$SCRIPT_DIR/build-ios-app.sh" "${@:2}"
    ;;
  *)
    echo "Usage: $0 <android|ios>" >&2
    exit 2
    ;;
esac
