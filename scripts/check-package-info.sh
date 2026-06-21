#!/usr/bin/env bash
#
# Fail the build if any documented package under CodenameOne/src is missing a
# package-info.java file.
#
# Background: packages without a package-info.java render in the published API
# reference with no description, and new packages (e.g. com.codename1.gpu, the
# box2d sub packages) repeatedly slipped in undocumented. This check makes that
# a build failure so the gap is caught in review rather than on the website.
#
# A "package" here is any directory that directly contains at least one .java
# file. The internal implementation package com.codename1.impl (and every sub
# package such as com.codename1.impl.gpu) is exempt: it is deliberately
# excluded from the published javadoc (see .github/scripts/build_javadocs.sh),
# so it needs no package description.
#
# Exit codes:
#   0 - every documented package has a package-info.java
#   1 - one or more documented packages are missing package-info.java
#   2 - misconfiguration (missing source directory)
#
# Usage:
#   scripts/check-package-info.sh [source-dir]
#
# When no argument is given the script scans CodenameOne/src relative to the
# repository root.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
SRC_DIR="${1:-$REPO_ROOT/CodenameOne/src}"

if [[ ! -d "$SRC_DIR" ]]; then
  echo "check-package-info: source directory not found: $SRC_DIR" >&2
  exit 2
fi

missing=0

# Enumerate every directory that directly contains a .java file. Sorting keeps
# the failure output deterministic.
while IFS= read -r dir; do
  # Skip the internal implementation package and all of its sub packages.
  case "$dir" in
    */com/codename1/impl|*/com/codename1/impl/*) continue ;;
  esac

  # A directory holds a real package only if it contains a .java file other
  # than package-info.java itself.
  if ! find "$dir" -maxdepth 1 -name '*.java' ! -name 'package-info.java' \
      -print -quit | grep -q .; then
    continue
  fi

  if [[ ! -f "$dir/package-info.java" ]]; then
    rel="${dir#"$REPO_ROOT"/}"
    printf '%s: missing package-info.java\n' "$rel"
    missing=$((missing + 1))
  fi
done < <(find "$SRC_DIR" -type d | sort)

if (( missing > 0 )); then
  echo "" >&2
  echo "check-package-info: $missing documented package(s) have no" >&2
  echo "  package-info.java. Add a package-info.java describing each package," >&2
  echo "  or, for an internal package, exclude it from the published javadoc." >&2
  exit 1
fi

echo "check-package-info: every documented package has a package-info.java."
