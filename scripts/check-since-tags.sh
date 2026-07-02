#!/usr/bin/env bash
#
# Scan Java sources under CodenameOne/src for @since javadoc tags whose
# referenced version does NOT correspond to a git tag in this repository.
#
# Background: Claude (and human contributors) sometimes write @since markers
# for releases that never shipped, leaving the API reference claiming a
# feature is available in versions readers cannot install. This check fails
# the build until every @since X.Y.Z matches an existing git tag
# (with or without a leading "v").
#
# Exit codes:
#   0 - all @since values match a tag
#   1 - one or more @since values do not match any tag
#   2 - misconfiguration (missing dirs, no tags fetched, etc.)
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

if ! command -v git >/dev/null 2>&1; then
  echo "check-since-tags: git is not on PATH; cannot enumerate tags." >&2
  exit 2
fi

# Build the allowed-versions set from `git tag`, stripping a leading "v" so
# both "v7.0" and "7.0" map to the same allowed value.
#
# We also seed the set with the "next patch" of every X.Y.Z tag because
# contributors necessarily write @since BEFORE the release that ships the
# feature is tagged. Concretely, if the highest 7.0.x tag is 7.0.244, we
# also accept @since 7.0.245 so the upcoming release can be referenced
# without flipping the build red on every PR. We deliberately do NOT
# auto-accept next-minor or next-major bumps (e.g. 7.1 or 8.0) — those
# require an explicit prior tag, because they are also exactly the values
# Claude hallucinates most often.
ALLOWED_TAGS_FILE="$(mktemp -t cn1-since-tags.XXXXXX)"
trap 'rm -f "$ALLOWED_TAGS_FILE"' EXIT

# Pipe through awk to add next-patch entries, then sort -u.
git -C "$REPO_ROOT" tag --list \
  | sed -E 's/^v//' \
  | awk '
      { print $0 }
      # Capture every X.Y.Z (numeric Z) and track the largest Z per X.Y.
      /^[0-9]+\.[0-9]+\.[0-9]+$/ {
        n = split($0, parts, ".")
        prefix = parts[1] "." parts[2]
        patch = parts[3] + 0
        if (patch > max_patch[prefix]) {
          max_patch[prefix] = patch
        }
      }
      END {
        # Emit one extra allowed version per release line: max+1.
        for (prefix in max_patch) {
          print prefix "." (max_patch[prefix] + 1)
        }
      }
  ' \
  | sort -u > "$ALLOWED_TAGS_FILE"

if [[ ! -s "$ALLOWED_TAGS_FILE" ]]; then
  echo "check-since-tags: no git tags found in $REPO_ROOT." >&2
  echo "  Make sure the workspace was cloned with tags (e.g. CI uses" >&2
  echo "  actions/checkout with fetch-tags: true)." >&2
  exit 2
fi

# Collect every '@since <version>' occurrence with file:line context.
# We deliberately match in any kind of comment (//, /** */, ///) because the
# tag is meaningful in all three forms.
HITS_FILE="$(mktemp -t cn1-since-hits.XXXXXX)"
trap 'rm -f "$ALLOWED_TAGS_FILE" "$HITS_FILE"' EXIT

# -E for ERE, -H to print filename, -n for line numbers, -r for recurse.
# The version captures digits.dots optionally followed by letters/dashes
# (e.g. "7.0.245", "3.5", "1.0-RC1"). A trailing period is stripped below.
#
# We deliberately require the @since to be preceded on the same line by a
# javadoc marker (`*` from /** */ blocks or `///` from Markdown javadoc).
# That excludes plain `// @since X.Y` code comments — which appear in
# vendored libraries (e.g. MiG Layout in ui/layouts/mig) where the value
# references the upstream library's changelog, not a Codename One release.
grep -EHnr --include='*.java' \
  '(\*|///).*@since[[:space:]]+[0-9][0-9A-Za-z._-]*' \
  "$SRC_DIR" > "$HITS_FILE" || true

violations=0

while IFS= read -r hit; do
  # hit format: path:line:full-line
  file="${hit%%:*}"
  rest="${hit#*:}"
  line="${rest%%:*}"
  text="${rest#*:}"

  # Extract every @since version on the line (a line may contain only one,
  # but the regex is tolerant of multiple).
  while IFS= read -r raw_version; do
    [[ -z "$raw_version" ]] && continue
    # Strip a trailing dot that is sentence punctuation, e.g.
    # "@since 3.5. Added the hint..."
    version="${raw_version%.}"
    if ! grep -Fxq "$version" "$ALLOWED_TAGS_FILE"; then
      printf '%s:%s: @since %s does not match any git tag\n' \
        "$file" "$line" "$version"
      violations=$((violations + 1))
    fi
  done < <(printf '%s\n' "$text" \
    | grep -oE '@since[[:space:]]+[0-9][0-9A-Za-z._-]*' \
    | sed -E 's/@since[[:space:]]+//')
done < "$HITS_FILE"

if (( violations > 0 )); then
  echo "" >&2
  echo "check-since-tags: $violations @since reference(s) point at versions" >&2
  echo "  with no matching git tag. Either fix the @since to a released" >&2
  echo "  version, or tag the release before merging." >&2
  exit 1
fi

echo "check-since-tags: all @since tags reference released versions."
