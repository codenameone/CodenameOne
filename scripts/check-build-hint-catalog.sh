#!/usr/bin/env bash
#
# Fails when code reads a build hint that the catalog does not describe.
#
# Build hints are string keys. Nothing checks them, so a hint that is misspelled
# where it is read -- or added to a builder and nowhere else -- simply does
# nothing: the build is green and the feature is inert. The catalog in
# maven/build-hint-catalog is what gives every hint a type, a default, a value
# domain and a doc row, and it is what the @Ios/@Android annotations are
# generated from. A hint missing from it is invisible to all of that.
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
