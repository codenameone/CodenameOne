#!/usr/bin/env bash
#
# Scan Java sources for @since javadoc tags whose referenced version does NOT
# correspond to a git tag in this repository.
#
# Background: Claude (and human contributors) sometimes write @since markers
# for releases that never shipped (e.g. "@since 8.0" when the latest release is
# 7.0.x), leaving the API reference claiming a feature is available in versions
# readers cannot install. This check fails the build until every such @since is
# corrected to a released version.
#
# Two scopes, two strictness levels:
#
#   * CodenameOne/src  -- STRICT. Every @since must match a released git tag
#     (or the next patch of an existing X.Y.Z line). This is hand-authored
#     framework API with no vendored code, so any stray @since is a real bug.
#
#   * Ports/**/com/codename1/** and maven/**/com/codename1/** -- LENIENT. The
#     platform ports also carry CN1-authored @since tags, but they live next to
#     vendored / adapted code that legitimately references Java or upstream
#     library versions (e.g. the adapted Base64 with "@since 1.3"). To catch the
#     hallucinated-release case without churning those legacy refs, lenient mode
#     only rejects a non-released @since whose LEADING version component is >=
#     the latest released major (e.g. 8.0, 7.1, or a bogus 12130) -- i.e. a
#     version claiming to be the current or a future Codename One release.
#     Anything below the current major (1.x .. 6.x) is tolerated.
#
# Vendored trees outside com/codename1 (org.cef, net.miginfocom, retroweaver,
# ...) are never scanned.
#
# Exit codes:
#   0 - all @since values are acceptable
#   1 - one or more @since values reference a non-existent release
#   2 - misconfiguration (missing git, no tags fetched, etc.)
#
# Usage:
#   scripts/check-since-tags.sh [source-dir]
#
# With an explicit source-dir argument the script scans that directory in
# STRICT mode (used by unit tests). With no argument it scans the framework
# core strictly and the CN1-authored port/maven packages leniently.
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

if ! command -v git >/dev/null 2>&1; then
  echo "check-since-tags: git is not on PATH; cannot enumerate tags." >&2
  exit 2
fi

# Build the allowed-versions set from `git tag`, stripping a leading "v" so
# both "v7.0" and "7.0" map to the same allowed value.
#
# We also seed the set with the "next patch" of every X.Y.Z tag because
# contributors necessarily write @since BEFORE the release that ships the
# feature is tagged. Concretely, if the highest 7.0.x tag is 7.0.255, we also
# accept @since 7.0.256. We deliberately do NOT auto-accept next-minor or
# next-major bumps (7.1 or 8.0) -- those require an explicit prior tag, because
# they are exactly the values that get hallucinated most often.
ALLOWED_TAGS_FILE="$(mktemp -t cn1-since-tags.XXXXXX)"
trap 'rm -f "$ALLOWED_TAGS_FILE"' EXIT

git -C "$REPO_ROOT" tag --list \
  | sed -E 's/^v//' \
  | awk '
      { print $0 }
      /^[0-9]+\.[0-9]+\.[0-9]+$/ {
        n = split($0, parts, ".")
        prefix = parts[1] "." parts[2]
        patch = parts[3] + 0
        if (patch > max_patch[prefix]) {
          max_patch[prefix] = patch
        }
      }
      END {
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

# Latest released MAJOR version (e.g. 7), computed only from X.Y[.Z]-shaped
# tags so a stray non-version tag cannot inflate the threshold. Lenient mode
# rejects any non-released @since whose leading component is >= this value.
LATEST_MAJOR="$(grep -E '^[0-9]+\.[0-9]+' "$ALLOWED_TAGS_FILE" \
  | sed -E 's/\..*$//' | sort -n | tail -1)"
LATEST_MAJOR="${LATEST_MAJOR:-0}"

violations=0

# check_tree <dir> <strict|lenient>
check_tree() {
  local dir="$1" mode="$2"
  [[ -d "$dir" ]] || return 0

  local hits
  # The version captures digits.dots optionally followed by letters/dashes
  # (e.g. "7.0.255", "3.5", "1.0-RC1"). A trailing sentence period is stripped
  # below. We require the @since to be preceded on the same line by a javadoc
  # marker (`*` from /** */ blocks or `///` from Markdown javadoc), which
  # excludes plain `// @since X.Y` code comments.
  hits="$(grep -EHnr --include='*.java' \
    '(\*|///).*@since[[:space:]]+[0-9][0-9A-Za-z._-]*' "$dir" 2>/dev/null || true)"
  [[ -z "$hits" ]] && return 0

  local hit file rest line text raw_version version leading
  while IFS= read -r hit; do
    [[ -z "$hit" ]] && continue
    file="${hit%%:*}"
    rest="${hit#*:}"
    line="${rest%%:*}"
    text="${rest#*:}"

    while IFS= read -r raw_version; do
      [[ -z "$raw_version" ]] && continue
      # Strip a trailing dot that is sentence punctuation.
      version="${raw_version%.}"
      grep -Fxq "$version" "$ALLOWED_TAGS_FILE" && continue

      if [[ "$mode" == "lenient" ]]; then
        leading="${version%%.*}"
        leading="${leading//[^0-9]/}"
        # Tolerate legacy / upstream version references below the current
        # major (Java's "@since 1.3", etc.). Only hallucinated current/future
        # releases (>= LATEST_MAJOR) are rejected here.
        [[ -n "$leading" ]] || continue
        (( leading >= LATEST_MAJOR )) || continue
      fi

      printf '%s:%s: @since %s does not match any released version\n' \
        "$file" "$line" "$version"
      violations=$((violations + 1))
    done < <(printf '%s\n' "$text" \
      | grep -oE '@since[[:space:]]+[0-9][0-9A-Za-z._-]*' \
      | sed -E 's/@since[[:space:]]+//')
  done <<< "$hits"
}

if [[ -n "${1:-}" ]]; then
  # Explicit directory: strict scan (backward compatible, used by tests).
  if [[ ! -d "$1" ]]; then
    echo "check-since-tags: source directory not found: $1" >&2
    exit 2
  fi
  check_tree "$1" strict
else
  check_tree "$REPO_ROOT/CodenameOne/src" strict
  # CN1-authored implementation code inside the platform ports + maven modules.
  while IFS= read -r d; do
    check_tree "$d" lenient
  done < <(find "$REPO_ROOT/Ports" "$REPO_ROOT/maven" \
            -type d -path '*/com/codename1' \
            -not -path '*/target/*' -not -path '*/build/*' -not -path '*/dist/*' \
            2>/dev/null | sort -u)
fi

if (( violations > 0 )); then
  echo "" >&2
  echo "check-since-tags: $violations @since reference(s) point at versions" >&2
  echo "  with no matching git tag. Either fix the @since to a released" >&2
  echo "  version, or tag the release before merging." >&2
  exit 1
fi

echo "check-since-tags: all @since tags reference released versions."
