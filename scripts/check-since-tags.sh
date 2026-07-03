#!/usr/bin/env bash
#
# Scan Java sources under CodenameOne/src for since markers in Javadoc or
# Markdown Javadoc comments.
#
# Background: generated documentation changes have repeatedly added @since
# tags and Markdown "Since" sections with guessed versions. Incorrect
# availability metadata is worse than no metadata, so public sources should
# not carry these markers at all.
#
# Exit codes:
#   0 - no since markers were found
#   1 - one or more since markers were found
#   2 - misconfiguration
#
# Usage:
#   scripts/check-since-tags.sh [source-dir]
#
# When no argument is given the script scans CodenameOne/src relative to
# the repository root.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
SRC_DIR="${1:-$REPO_ROOT/CodenameOne/src}"

if [[ ! -d "$SRC_DIR" ]]; then
  echo "check-since-tags: source directory not found: $SRC_DIR" >&2
  exit 2
fi

HITS_FILE="$(mktemp -t cn1-since-hits.XXXXXX)"
trap 'rm -f "$HITS_FILE"' EXIT

# Match both classic tags and generated Markdown sections such as:
#   /// @since 7.0.245
#   /// #### Since
#   * Since
#   // @since 3.7.2 ...
grep -EHinr --include='*.java' \
  '(@since|^[[:space:]]*(///|//|/\*+|\*)[[:space:]]*(#+[[:space:]]*)?since[[:space:]:.]*$)' \
  "$SRC_DIR" > "$HITS_FILE" || true

if [[ -s "$HITS_FILE" ]]; then
  cat "$HITS_FILE"
  echo "" >&2
  echo "check-since-tags: since markers are not allowed in CodenameOne API docs." >&2
  echo "  Remove @since tags and Markdown/Javadoc 'Since' sections instead of" >&2
  echo "  guessing release versions." >&2
  exit 1
fi

echo "check-since-tags: no since markers found."
