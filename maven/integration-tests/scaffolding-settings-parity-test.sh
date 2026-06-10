#!/usr/bin/env bash
#
# scaffolding-settings-parity-test.sh
#
# Guards the invariant that the two project-scaffolding paths ship the SAME set of
# build-hint properties in codenameone_settings.properties:
#
#   1. Maven archetype  : maven/cn1app-archetype/.../archetype-resources/common/codenameone_settings.properties
#   2. Initializr        : the common/codenameone_settings.properties embedded in
#                          scripts/initializr/common/src/main/resources/common.zip
#
# A newly-generated project must look identical regardless of which path produced it.
# Historically the two drifted: iOS/Android on-device-debug hints (ios.onDeviceDebug,
# android.onDeviceDebug, ...) and others were added to the archetype but never to the
# initializr's common.zip, so they were silently missing from initializr projects.
#
# We compare the *property/hint key+value set* (commented-out hint lines included),
# after canonicalising the per-project placeholders (app name, package, java version)
# and ignoring comment prose / ordering. The two scaffolds only differ in those
# placeholders, so any other difference is real drift and fails the build.
#
# Exit codes: 0 = in sync, 1 = drift detected, 2 = setup error.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ARCHETYPE_SETTINGS="$ROOT/maven/cn1app-archetype/src/main/resources/archetype-resources/common/codenameone_settings.properties"
INITIALIZR_ZIP="$ROOT/scripts/initializr/common/src/main/resources/common.zip"
INITIALIZR_ZIP_ENTRY="common/codenameone_settings.properties"

fail_setup() { echo "ERROR (setup): $1" >&2; exit 2; }

command -v python3 >/dev/null 2>&1 || fail_setup "python3 is required"
command -v unzip   >/dev/null 2>&1 || fail_setup "unzip is required"
[ -f "$ARCHETYPE_SETTINGS" ] || fail_setup "archetype settings not found: $ARCHETYPE_SETTINGS"
[ -f "$INITIALIZR_ZIP" ]     || fail_setup "initializr common.zip not found: $INITIALIZR_ZIP"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

INITIALIZR_SETTINGS="$TMP_DIR/initializr-settings.properties"
if ! unzip -p "$INITIALIZR_ZIP" "$INITIALIZR_ZIP_ENTRY" > "$INITIALIZR_SETTINGS" 2>/dev/null; then
  fail_setup "could not extract $INITIALIZR_ZIP_ENTRY from $INITIALIZR_ZIP"
fi
[ -s "$INITIALIZR_SETTINGS" ] || fail_setup "extracted initializr settings is empty"

python3 "$SCRIPT_DIR/normalize_cn1_settings.py" \
  --archetype "$ARCHETYPE_SETTINGS" \
  --initializr "$INITIALIZR_SETTINGS"
