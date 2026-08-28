#!/usr/bin/env bash
#
# Fails when code reads a build hint that the catalog does not describe.
#
# Build hints are string keys. Nothing checks them, so a hint that is misspelled
# where it is read -- or added to a builder and nowhere else -- simply does
# nothing: the build is green and the feature is inert. Every hint must be
# declared -- by an annotation in CodenameOne/src/com/codename1/annotations/
# buildhints, or in maven/build-hint-catalog when it has none -- and that
# declaration is what gives it a type, a value domain and a doc row. A hint
# missing from both is invisible to all of that.
#
#   scripts/check-build-hint-catalog.sh [--write-baseline]
#
# The result is held against scripts/build-hint-catalog-baseline.txt, a ratchet
# of pre-existing debt rather than an allow-list. That file is currently empty:
# every hint the code reads is described. Keep it that way -- a new entry means
# a new hint went in without a catalog row.
#
# Reads source, not bytecode, so nothing has to be built first and no module can
# silently drop out of coverage.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec python3 "$SCRIPT_DIR/check-build-hint-catalog.py" "$@"
